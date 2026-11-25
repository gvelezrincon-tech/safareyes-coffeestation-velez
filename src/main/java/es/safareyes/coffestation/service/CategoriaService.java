package es.safareyes.coffestation.service;

import es.safareyes.coffestation.dto.request.DisponibilidadRequest;
import es.safareyes.coffestation.dto.response.CategoriaResponse;

import java.util.List;

// lo que se puede hacer con las categorías (endpoints 7 y 8)
public interface CategoriaService {

    // endpoint 7: todas las categorías con su nº de productos activos
    List<CategoriaResponse> listar();

    // endpoint 8: agotar o reponer de golpe todos los productos de una categoría
    CategoriaResponse cambiarDisponibilidad(Long id, DisponibilidadRequest request);
}
