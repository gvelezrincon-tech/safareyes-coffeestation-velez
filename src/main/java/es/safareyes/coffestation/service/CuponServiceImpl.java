package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.Cliente;
import es.safareyes.coffestation.Modelo.Cupones;
import es.safareyes.coffestation.Modelo.TipoCupon;
import es.safareyes.coffestation.Repositorio.Clienterepo;
import es.safareyes.coffestation.Repositorio.Cuponesrepo;
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
import java.time.LocalDateTime;

// lógica de los cupones
@Slf4j
@Service
@RequiredArgsConstructor
public class CuponServiceImpl implements CuponService {

    private final Cuponesrepo cuponesrepo;
    private final Clienterepo clienterepo;
    private final CuponMapper cuponMapper;


    // endpoint 13 - POST /api/v1/cupones
    // url: http://localhost:8080/api/v1/cupones
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
    // url: http://localhost:8080/api/v1/cupones/validar   cuerpo: { "codigo": "BIENVENIDA10", "subtotal": 12.50 }
    //
    // aquí un cupón que no vale NO es un error: la pregunta "¿vale?" se responde siempre con 200,
    // y en la respuesta va valido = true/false y un mensaje diciendo por qué.
    //
    // RN-06: un cupón vale si...
    //   1. existe                                     -> hecho
    //   2. está activo                                -> hecho
    //   3. hoy está dentro de sus fechas              -> hecho
    //   4. no ha llegado a sus usos máximos           -> TODO
    //   5. el subtotal supera su importe mínimo       -> TODO
    //   6. si es personal, el email es el del cliente -> TODO (fase 4, hace falta saber quién está logueado)
    // y si vale, hay que calcular el descuento        -> TODO
    @Override
    @Transactional(readOnly = true)
    public ValidarCuponResponse validar(ValidarCuponRequest request) {
        String codigo = request.codigo().trim().toUpperCase();
        log.info("Validar cupón {} para un subtotal de {}", codigo, request.subtotal());

        // 1. existe
        Cupones cupon = cuponesrepo.findByCodigoIgnoreCase(codigo).orElse(null);
        if (cupon == null) {
            return noValido("No existe ningún cupón con el código " + codigo);
        }

        // 2. está activo
        if (!cupon.getActivo()) {
            return noValido("El cupón " + codigo + " está desactivado");
        }

        // 3. hoy está dentro de sus fechas (ni antes de empezar ni después de acabar)
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(cupon.getFechaInicio())) {
            return noValido("El cupón " + codigo + " todavía no ha empezado (empieza el "
                    + cupon.getFechaInicio().toLocalDate() + ")");
        }
        if (ahora.isAfter(cupon.getFechaFin())) {
            return noValido("El cupón " + codigo + " está caducado (acabó el "
                    + cupon.getFechaFin().toLocalDate() + ")");
        }

        // TODO 4. usos máximos: contar las filas del cupón en pedido_cupones SIN contar los pedidos
        //  CANCELADO (RN-08: cancelar devuelve el uso). hace falta una consulta nueva en PedidoCuponesrepo

        // TODO 5. importe mínimo: si el cupón tiene importeMinimo (puede ser null),
        //  el subtotal tiene que ser MAYOR (no igual) que él

        // TODO 6. cupón personal: si cupon.getCliente() no es null, comparar su email con el del
        //  cliente que hace la petición. eso se sabe con el JWT, así que se queda para la fase 4

        // TODO descuento: PORCENTAJE -> subtotal * valor / 100; IMPORTE_FIJO -> valor.
        //  redondeado a 2 decimales y nunca más grande que el subtotal (RN-07: el total no puede ser negativo)
        BigDecimal descuento = BigDecimal.ZERO;

        return new ValidarCuponResponse(true, "Cupón válido", descuento);
    }

    // respuesta de "este cupón no vale": sin descuento y con el motivo
    private ValidarCuponResponse noValido(String motivo) {
        log.info("Cupón no válido: {}", motivo);
        return new ValidarCuponResponse(false, motivo, BigDecimal.ZERO);
    }
}
