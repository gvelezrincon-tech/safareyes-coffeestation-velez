package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.Cliente;
import es.safareyes.coffestation.Modelo.Cupones;
import es.safareyes.coffestation.Modelo.EstadoPedido;
import es.safareyes.coffestation.Modelo.Pedido;
import es.safareyes.coffestation.Modelo.PedidoCupones;
import es.safareyes.coffestation.Modelo.Producto;
import es.safareyes.coffestation.Modelo.ProductoPedido;
import es.safareyes.coffestation.Repositorio.Clienterepo;
import es.safareyes.coffestation.Repositorio.Pedidorepo;
import es.safareyes.coffestation.Repositorio.Productorepo;
import es.safareyes.coffestation.dto.request.EstadoPedidoRequest;
import es.safareyes.coffestation.dto.request.LineaPedidoRequest;
import es.safareyes.coffestation.dto.request.PedidoRequest;
import es.safareyes.coffestation.dto.response.PedidoResponse;
import es.safareyes.coffestation.exception.RecursoNoEncontradoException;
import es.safareyes.coffestation.exception.ReglaNegocioException;
import es.safareyes.coffestation.mapper.PedidoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// lógica de los pedidos (endpoints 9, 10, 11 y 12)
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    // RN-05: los precios llevan el 10 % de iva dentro
    private static final BigDecimal DIVISOR_IVA = new BigDecimal("1.10");
    // RN-04: unidades máximas de un producto en un pedido
    private static final int MAX_UNIDADES_LINEA = 20;
    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    private final Pedidorepo pedidorepo;
    private final Productorepo productorepo;
    private final Clienterepo clienterepo;
    private final CuponService cuponService;
    private final PedidoMapper pedidoMapper;


    // endpoint 9 - POST /api/v1/pedidos
    // url: http://localhost:8081/api/v1/pedidos
    // cuerpo: { "clienteId": 3, "lineas": [ { "productoId": 2, "cantidad": 2 } ], "codigoCupon": "OTONO15" }
    // (clienteId y codigoCupon son opcionales)
    //
    // pasos:
    //   1. juntar las líneas del mismo producto (RN-04)
    //   2. comprobar los productos: 404 si no existen, 422 si están de baja o agotados (RN-01, RN-02)
    //   3. subtotal con el precio de ahora de cada producto (RN-03)
    //   4. descuento del cupón, si llega (RN-06). nunca más que el subtotal (RN-07)
    //   5. total, base e iva (RN-05) y número de turno del día (RN-09)
    //   6. guardar el pedido con sus líneas y el cupón usado
    //
    // TODO fase 5: dniSocio todavía no se usa. cuando esté FitZone (contrato B), preguntarle si el DNI
    //   es de un socio activo y, si lo es, aplicar su 10 % o el cupón, el que sea mayor (RN-07)
    // TODO fase 4: si quien pide es un CLIENTE, el cliente sale de su token y no del cuerpo
    @Override
    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        log.info("Nuevo pedido con {} líneas", request.lineas().size());

        // el cliente es opcional: un pedido de barra lo puede tomar el barista sin cliente registrado
        Cliente cliente = null;
        if (request.clienteId() != null) {
            cliente = clienterepo.findById(request.clienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el cliente con id " + request.clienteId()));
        }

        // 1 y 2
        Map<Long, Integer> cantidades = agruparLineas(request.lineas());
        Map<Long, Producto> productos = comprobarProductos(cantidades);

        // 3. subtotal = suma de precio * cantidad
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> linea : cantidades.entrySet()) {
            BigDecimal precio = productos.get(linea.getKey()).getPrecio();
            subtotal = subtotal.add(precio.multiply(BigDecimal.valueOf(linea.getValue())));
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        // 4. cupón (opcional). si no vale, el pedido no se crea: 422 con el motivo.
        // calcularDescuento ya se asegura de que no descuente más que el subtotal (RN-07)
        Cupones cupon = null;
        BigDecimal descuento = BigDecimal.ZERO;
        if (request.codigoCupon() != null && !request.codigoCupon().isBlank()) {
            String email = (cliente != null) ? cliente.getEmail() : null;
            cupon = cuponService.buscarCuponAplicable(request.codigoCupon(), subtotal, email);
            descuento = cuponService.calcularDescuento(cupon, subtotal);
        }
        log.info("Subtotal {} €, descuento {} €", subtotal, descuento);

        // 5. RN-05: el total nunca baja de 0. base = total / 1,10 y el iva es lo que falta hasta el total
        // (así base + iva da siempre el total exacto, sin líos de redondeo)
        BigDecimal total = subtotal.subtract(descuento).max(BigDecimal.ZERO);
        BigDecimal base = total.divide(DIVISOR_IVA, 2, RoundingMode.HALF_UP);
        BigDecimal iva = total.subtract(base);

        // RN-09: turno = pedidos creados hoy + 1 (mañana vuelve a empezar en 1)
        LocalDateTime ahora = LocalDateTime.now(ZONA).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime inicioDia = ahora.toLocalDate().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1).minusNanos(1);
        long turno = pedidorepo.countByFechaHoraBetween(inicioDia, finDia) + 1;

        // 6. el pedido con todo lo de dentro. con el cascade PERSIST de Pedido, un solo save()
        // guarda también las líneas y el cupón usado
        Pedido pedido = new Pedido();
        pedido.setNumeroTurno(turno);
        pedido.setFechaHora(ahora);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setCliente(cliente);
        pedido.setBase(base);
        pedido.setIva(iva);

        for (Map.Entry<Long, Integer> entrada : cantidades.entrySet()) {
            Producto producto = productos.get(entrada.getKey());
            ProductoPedido linea = new ProductoPedido();
            linea.setPedido(pedido);
            linea.setProducto(producto);
            linea.setCantidad(entrada.getValue().longValue());
            // RN-03: se guarda el precio de hoy; si mañana cambia, este pedido no se entera
            linea.setPrecioUnitario(producto.getPrecio());
            pedido.getLineas().add(linea);
        }

        if (cupon != null) {
            // el uso del cupón es esta fila de pedido_cupones (si el pedido se cancela, deja de contar)
            PedidoCupones usoCupon = new PedidoCupones();
            usoCupon.setPedido(pedido);
            usoCupon.setCupon(cupon);
            usoCupon.setDescuento(descuento);
            pedido.getCupones().add(usoCupon);
        }

        Pedido guardado = pedidorepo.save(pedido);
        log.info("Pedido {} creado con turno {} y total {} €", guardado.getId(), turno, total);
        return pedidoMapper.toResponse(guardado);
    }


    // endpoint 10 - GET /api/v1/pedidos/{id}
    // url: http://localhost:8081/api/v1/pedidos/10
    // TODO fase 4: un CLIENTE solo puede ver sus propios pedidos
    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPorId(Long id) {
        log.info("Detalle del pedido {}", id);

        // el @EntityGraph del repo ya trae cliente, líneas y productos de golpe
        Pedido pedido = buscarPedido(id);
        return pedidoMapper.toResponse(pedido);
    }


    // endpoint 11 - GET /api/v1/pedidos
    // url: http://localhost:8081/api/v1/pedidos?estado=PENDIENTE&fecha=2026-11-19&page=0&size=10
    // TODO fase 4: un CLIENTE solo ve los suyos
    //
    // aquí el @Transactional(readOnly = true) hace falta de verdad: las líneas son LAZY
    // y el mapper las pide al convertir cada pedido, así que la conexión tiene que seguir abierta
    @Override
    @Transactional(readOnly = true)
    public Page<PedidoResponse> listar(EstadoPedido estado, LocalDate fecha, Pageable pageable) {
        log.info("Listado de pedidos: estado={}, fecha={}, pagina={}", estado, fecha, pageable);

        // los dos filtros son opcionales, así que hay 4 casos y cada uno usa su consulta
        Page<Pedido> pedidos;
        if (fecha == null) {
            pedidos = (estado == null)
                    ? pedidorepo.findAll(pageable)
                    : pedidorepo.findByEstado(estado, pageable);
        } else {
            // el día entero: desde las 00:00 hasta justo antes de las 00:00 del día siguiente
            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.plusDays(1).atStartOfDay().minusNanos(1);
            pedidos = (estado == null)
                    ? pedidorepo.findByFechaHoraBetween(inicio, fin, pageable)
                    : pedidorepo.findByEstadoAndFechaHoraBetween(estado, inicio, fin, pageable);
        }

        return pedidos.map(pedidoMapper::toResponse);
    }


    // endpoint 12 - PATCH /api/v1/pedidos/{id}/estado
    // url: http://localhost:8081/api/v1/pedidos/10/estado   cuerpo: { "estado": "EN_PREPARACION" }
    //
    // RN-08: solo vale PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO y PENDIENTE -> CANCELADO.
    // cualquier otro salto (LISTO -> PENDIENTE, ENTREGADO -> CANCELADO...) es un 422.
    // (sin save(): el pedido viene de la bd dentro de la transacción y Hibernate guarda el cambio solo)
    //
    // "cancelar devuelve el uso al cupón": aquí no hay que tocar nada. los usos de un cupón salen
    // de contar sus filas en pedido_cupones sin los pedidos CANCELADO (lo hace CuponServiceImpl)
    @Override
    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedidoRequest request) {
        Pedido pedido = buscarPedido(id);

        // el dto ya ha comprobado que el texto es uno de los 5 estados, así que valueOf no falla
        EstadoPedido actual = pedido.getEstado();
        EstadoPedido nuevo = EstadoPedido.valueOf(request.estado());
        log.info("Pedido {}: {} -> {}", id, actual, nuevo);

        if (!actual.puedeCambiarA(nuevo)) {
            throw new ReglaNegocioException("El pedido " + id + " no puede pasar de "
                    + actual + " a " + nuevo);
        }

        pedido.setEstado(nuevo);
        return pedidoMapper.toResponse(pedido);
    }


    // 404 si el pedido no existe. trae también cliente, líneas y productos para el mapper
    private Pedido buscarPedido(Long id) {
        return pedidorepo.obtenerPedidoConLineas(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido con id " + id));
    }

    // RN-04: si el mismo producto llega en dos líneas, se juntan en una sumando las cantidades.
    // { 2: 1 unidad }, { 2: 3 unidades } -> { 2: 4 unidades }
    // LinkedHashMap para que las líneas salgan en el mismo orden en que llegaron.
    // si al juntarlas pasan de 20 unidades, 422
    private Map<Long, Integer> agruparLineas(List<LineaPedidoRequest> lineas) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (LineaPedidoRequest linea : lineas) {
            cantidades.merge(linea.productoId(), linea.cantidad(), Integer::sum);
        }

        cantidades.forEach((productoId, cantidad) -> {
            if (cantidad > MAX_UNIDADES_LINEA) {
                throw new ReglaNegocioException("Del producto " + productoId + " se piden " + cantidad
                        + " unidades en total y el máximo por línea es " + MAX_UNIDADES_LINEA);
            }
        });
        return cantidades;
    }

    // trae todos los productos del pedido en una sola consulta y los comprueba:
    //   404 si alguno no existe
    //   422 si está dado de baja (RN-01) o agotado (RN-02), diciendo cuál
    private Map<Long, Producto> comprobarProductos(Map<Long, Integer> cantidades) {
        Map<Long, Producto> productos = productorepo.findAllById(cantidades.keySet()).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        for (Long productoId : cantidades.keySet()) {
            Producto producto = productos.get(productoId);
            if (producto == null) {
                throw new RecursoNoEncontradoException("No existe el producto con id " + productoId);
            }
            if (!producto.getActivo()) {
                throw new ReglaNegocioException("El producto " + producto.getNombre()
                        + " (id " + productoId + ") está dado de baja y no se puede pedir");
            }
            if (!producto.getDisponible()) {
                throw new ReglaNegocioException("El producto " + producto.getNombre()
                        + " (id " + productoId + ") está agotado");
            }
        }
        return productos;
    }
}
