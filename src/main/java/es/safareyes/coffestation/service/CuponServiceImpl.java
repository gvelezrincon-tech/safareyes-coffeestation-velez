package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.Cliente;
import es.safareyes.coffestation.Modelo.Cupones;
import es.safareyes.coffestation.Modelo.TipoCupon;
import es.safareyes.coffestation.Repositorio.Clienterepo;
import es.safareyes.coffestation.Repositorio.Cuponesrepo;
import es.safareyes.coffestation.dto.request.CuponRequest;
import es.safareyes.coffestation.dto.response.CuponResponse;
import es.safareyes.coffestation.exception.RecursoDuplicadoException;
import es.safareyes.coffestation.exception.RecursoNoEncontradoException;
import es.safareyes.coffestation.exception.ReglaNegocioException;
import es.safareyes.coffestation.mapper.CuponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
}
