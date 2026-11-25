package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.EstadoPedido;
import es.safareyes.coffestation.Modelo.Pedido;
import es.safareyes.coffestation.Repositorio.Pedidorepo;
import es.safareyes.coffestation.dto.request.EstadoPedidoRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

// lógica de los pedidos (endpoints 10, 11 y 12)
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final Pedidorepo pedidorepo;
    private final PedidoMapper pedidoMapper;


    // endpoint 10 - GET /api/v1/pedidos/{id}
    // url: http://localhost:8080/api/v1/pedidos/10
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
    // url: http://localhost:8080/api/v1/pedidos?estado=PENDIENTE&fecha=2026-11-19&page=0&size=10
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
    // url: http://localhost:8080/api/v1/pedidos/10/estado   cuerpo: { "estado": "EN_PREPARACION" }
    //
    // RN-08: solo vale PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO y PENDIENTE -> CANCELADO.
    // cualquier otro salto (LISTO -> PENDIENTE, ENTREGADO -> CANCELADO...) es un 422.
    // (sin save(): el pedido viene de la bd dentro de la transacción y Hibernate guarda el cambio solo)
    //
    // TODO endpoint 9/13/14: "cancelar devuelve el uso al cupón". aquí no hay que tocar nada:
    // como los usos de un cupón salen de contar sus filas en pedido_cupones, al validar el cupón
    // habrá que contar solo las de pedidos que no estén CANCELADO
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
}
