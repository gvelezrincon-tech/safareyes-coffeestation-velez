package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.Cupones;
import es.safareyes.coffestation.dto.request.CuponRequest;
import es.safareyes.coffestation.dto.request.ValidarCuponRequest;
import es.safareyes.coffestation.dto.response.CuponResponse;
import es.safareyes.coffestation.dto.response.ValidarCuponResponse;

import java.math.BigDecimal;

// lo que se puede hacer con los cupones (endpoints 13 y 14, y lo que usa el 9 al crear un pedido)
public interface CuponService {

    // endpoint 13: crear un cupón
    CuponResponse crear(CuponRequest request);

    // endpoint 14: ¿vale este cupón para este subtotal? y si vale, ¿cuánto descuenta?
    ValidarCuponResponse validar(ValidarCuponRequest request);

    // endpoint 9: el cupón que llega al crear un pedido. si no vale, 422 con el motivo
    Cupones buscarCuponAplicable(String codigo, BigDecimal subtotal, String email);

    // cuánto descuenta un cupón sobre un subtotal (nunca más que el subtotal)
    BigDecimal calcularDescuento(Cupones cupon, BigDecimal subtotal);
}
