package es.safareyes.coffestation.service;

import es.safareyes.coffestation.dto.request.CuponRequest;
import es.safareyes.coffestation.dto.response.CuponResponse;

// lo que se puede hacer con los cupones (de momento el endpoint 13)
public interface CuponService {

    // endpoint 13: crear un cupón
    CuponResponse crear(CuponRequest request);
}
