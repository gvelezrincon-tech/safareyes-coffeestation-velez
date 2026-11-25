package es.safareyes.coffestation.service;

import es.safareyes.coffestation.dto.request.CuponRequest;
import es.safareyes.coffestation.dto.request.ValidarCuponRequest;
import es.safareyes.coffestation.dto.response.CuponResponse;
import es.safareyes.coffestation.dto.response.ValidarCuponResponse;

// lo que se puede hacer con los cupones (endpoints 13 y 14)
public interface CuponService {

    // endpoint 13: crear un cupón
    CuponResponse crear(CuponRequest request);

    // endpoint 14: ¿vale este cupón para este subtotal? y si vale, ¿cuánto descuenta?
    ValidarCuponResponse validar(ValidarCuponRequest request);
}
