package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.Cliente;
import es.safareyes.coffestation.Modelo.Cupones;
import es.safareyes.coffestation.Modelo.EstadoPedido;
import es.safareyes.coffestation.Modelo.TipoCupon;
import es.safareyes.coffestation.Repositorio.Clienterepo;
import es.safareyes.coffestation.Repositorio.Cuponesrepo;
import es.safareyes.coffestation.Repositorio.PedidoCuponesrepo;
import es.safareyes.coffestation.dto.request.CuponRequest;
import es.safareyes.coffestation.dto.request.ValidarCuponRequest;
import es.safareyes.coffestation.dto.response.CuponResponse;
import es.safareyes.coffestation.dto.response.ValidarCuponResponse;
import es.safareyes.coffestation.exception.RecursoDuplicadoException;
import es.safareyes.coffestation.exception.RecursoNoEncontradoException;
import es.safareyes.coffestation.exception.ReglaNegocioException;
import es.safareyes.coffestation.mapper.CuponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

// lógica de los cupones
@Slf4j
@Service
@RequiredArgsConstructor
public class CuponServiceImpl implements CuponService {

    private final Cuponesrepo cuponesrepo;
    private final PedidoCuponesrepo pedidoCuponesrepo;
    private final Clienterepo clienterepo;
    private final CuponMapper cuponMapper;


    // endpoint 13 - POST /api/v1/cupones
    // url: http://localhost:8081/api/v1/cupones
    //
    // el dto ya ha comprobado lo "de formato" (campos obligatorios, valor > 0, formato del código...).
    // aquí van las comprobaciones que necesitan pensar un poco o mirar la bd:
    //   409 código repetido, 422 fechas al revés, 422 porcentaje de más de 100, 404 cliente que no existe
    @Override
    @Transactional
    public CuponResponse crear(CuponRequest request) {
        // los códigos se guardan siempre en mayúsculas: "otono15" -> "OTONO15"
        String codigo = request.codigo().trim().toUpperCase();
        log.info("Alta del cupón {}", codigo);

        if (cuponesrepo.existsByCodigoIgnoreCase(codigo)) {
            throw new RecursoDuplicadoException("Ya existe un cupón con el código " + codigo);
        }

        // un cupón tiene que acabar después de empezar
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin del cupón tiene que ser posterior a la de inicio");
        }

        // un descuento de más del 100 % dejaría el pedido en negativo
        TipoCupon tipo = TipoCupon.valueOf(request.tipo());
        if (tipo == TipoCupon.PORCENTAJE && request.valor().compareTo(new BigDecimal("100")) > 0) {
            throw new ReglaNegocioException("Un cupón de porcentaje no puede descontar más del 100 %");
        }

        Cupones cupon = cuponMapper.toEntity(request);
        cupon.setCodigo(codigo);
        cupon.setActivo(true);

        // si llega clienteId, el cupón es personal (RN-06: solo lo podrá usar ese cliente)
        if (request.clienteId() != null) {
            Cliente cliente = clienterepo.findById(request.clienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el cliente con id " + request.clienteId()));
            cupon.setCliente(cliente);
        }

        Cupones guardado = cuponesrepo.save(cupon);
        log.info("Cupón {} creado con id {}", codigo, guardado.getId());
        return cuponMapper.toResponse(guardado);
    }


    // endpoint 14 - POST /api/v1/cupones/validar
    // url: http://localhost:8081/api/v1/cupones/validar   cuerpo: { "codigo": "BIENVENIDA10", "subtotal": 12.50 }
    //
    // aquí un cupón que no vale NO es un error: la pregunta "¿vale?" se responde siempre con 200,
    // y en la respuesta va valido = true/false y un mensaje diciendo por qué.
    // las comprobaciones de la RN-06 están en motivoNoValido (las comparte con el endpoint 9)
    @Override
    @Transactional(readOnly = true)
    public ValidarCuponResponse validar(ValidarCuponRequest request) {
        String codigo = request.codigo().trim().toUpperCase();
        log.info("Validar cupón {} para un subtotal de {}", codigo, request.subtotal());

        Cupones cupon = cuponesrepo.findByCodigoIgnoreCase(codigo).orElse(null);
        String motivo = motivoNoValido(cupon, codigo, request.subtotal(), request.email());
        if (motivo != null) {
            return noValido(motivo);
        }

        // ha pasado todo: el cupón vale. calculamos cuánto descuenta
        BigDecimal descuento = calcularDescuento(cupon, request.subtotal());
        log.info("Cupón {} válido: descuenta {} €", codigo, descuento);
        return new ValidarCuponResponse(true, "Cupón válido", descuento);
    }


    // endpoint 9 - POST /api/v1/pedidos con "codigoCupon"
    // mismas comprobaciones que el endpoint 14, pero aquí un cupón que no vale sí es un error:
    // el pedido no se crea y sale un 422 con el motivo (el barista avisa al cliente y lo vuelve a pedir)
    // sin readOnly: se llama desde dentro de la transacción de crear el pedido, que sí escribe
    @Override
    @Transactional
    public Cupones buscarCuponAplicable(String codigo, BigDecimal subtotal, String email) {
        String codigoLimpio = codigo.trim().toUpperCase();
        log.info("Cupón {} en un pedido de {} €", codigoLimpio, subtotal);

        Cupones cupon = cuponesrepo.findByCodigoIgnoreCase(codigoLimpio).orElse(null);
        String motivo = motivoNoValido(cupon, codigoLimpio, subtotal, email);
        if (motivo != null) {
            throw new ReglaNegocioException(motivo);
        }
        return cupon;
    }


    // cuánto descuenta un cupón sobre un subtotal:
    //   PORCENTAJE   -> subtotal * valor / 100   (10 % de 12,50 = 1,25)
    //   IMPORTE_FIJO -> el valor tal cual        (1 € = 1,00)
    // redondeado a 2 decimales (HALF_UP = el redondeo de toda la vida: 0,125 -> 0,13).
    // nunca más que el subtotal: un cupón de 5 € en un pedido de 3 € descuenta 3 €,
    // así el total no se queda en negativo (RN-07).
    // lo usan el endpoint 14 y el 9 (al crear el pedido)
    @Override
    public BigDecimal calcularDescuento(Cupones cupon, BigDecimal subtotal) {
        BigDecimal descuento = (cupon.getTipo() == TipoCupon.PORCENTAJE)
                ? subtotal.multiply(cupon.getValor()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : cupon.getValor().setScale(2, RoundingMode.HALF_UP);
        return descuento.min(subtotal);
    }


    // RN-06: un cupón vale si...
    //   1. existe
    //   2. está activo
    //   3. hoy está dentro de sus fechas
    //   4. no ha llegado a sus usos máximos
    //   5. el subtotal supera su importe mínimo
    //   6. si es personal, el email es el del cliente
    // se comprueban en ese orden y en cuanto una falla se devuelve el motivo.
    // si pasa las 6, devuelve null (= el cupón vale)
    private String motivoNoValido(Cupones cupon, String codigo, BigDecimal subtotal, String email) {
        // 1. existe
        if (cupon == null) {
            return "No existe ningún cupón con el código " + codigo;
        }

        // 2. está activo
        if (!cupon.getActivo()) {
            return "El cupón " + codigo + " está desactivado";
        }

        // 3. hoy está dentro de sus fechas (ni antes de empezar ni después de acabar)
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(cupon.getFechaInicio())) {
            return "El cupón " + codigo + " todavía no ha empezado (empieza el "
                    + cupon.getFechaInicio().toLocalDate() + ")";
        }
        if (ahora.isAfter(cupon.getFechaFin())) {
            return "El cupón " + codigo + " está caducado (acabó el "
                    + cupon.getFechaFin().toLocalDate() + ")";
        }

        // 4. no ha llegado a sus usos máximos.
        // los usos son sus filas en pedido_cupones, pero sin contar los pedidos CANCELADO
        // (RN-08: cancelar un pedido le devuelve el uso al cupón)
        long usos = pedidoCuponesrepo.countByCuponIdAndPedidoEstadoNot(cupon.getId(), EstadoPedido.CANCELADO);
        if (usos >= cupon.getMaxUsos()) {
            return "El cupón " + codigo + " ya ha llegado a su máximo de usos (" + cupon.getMaxUsos() + ")";
        }

        // 5. el subtotal SUPERA el importe mínimo (si es igual, no vale).
        // importeMinimo es opcional: si es null, el cupón vale para cualquier importe
        if (cupon.getImporteMinimo() != null && subtotal.compareTo(cupon.getImporteMinimo()) <= 0) {
            return "El cupón " + codigo + " solo vale para pedidos de más de " + cupon.getImporteMinimo() + " €";
        }

        // 6. si el cupón es personal, solo lo puede usar su cliente (se comparan los emails).
        // en el 14 el email llega en la petición y en el 9 es el del cliente del pedido.
        // en la fase 4 un CLIENTE lo sacará de su token
        // ojo: cupon.getCliente() es LAZY, pero estamos dentro de la transacción y se carga sin problema
        if (cupon.getCliente() != null) {
            String emailCupon = cupon.getCliente().getEmail();
            if (email == null || !emailCupon.equalsIgnoreCase(email.trim())) {
                return "El cupón " + codigo + " es personal y no pertenece a este cliente";
            }
        }

        return null;
    }

    // respuesta de "este cupón no vale": sin descuento y con el motivo
    private ValidarCuponResponse noValido(String motivo) {
        log.info("Cupón no válido: {}", motivo);
        return new ValidarCuponResponse(false, motivo, BigDecimal.ZERO);
    }
}
