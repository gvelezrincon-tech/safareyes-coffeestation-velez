package es.safareyes.coffestation.controller;

import es.safareyes.coffestation.dto.request.DisponibilidadRequest;
import es.safareyes.coffestation.dto.response.CategoriaResponse;
import es.safareyes.coffestation.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// endpoints de las categorías (7 y 8)
//
// roles (fase 4):
//   GET   -> público
//   PATCH -> ADMIN, BARISTA
@Slf4j
@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    // 7. GET /api/v1/categorias
    // 200 con todas las categorías y su nº de productos activos (las que no tienen ninguno salen con 0)
    @GetMapping
    public List<CategoriaResponse> listar() {
        log.info("GET /categorias");
        return categoriaService.listar();
    }

    // 8. PATCH /api/v1/categorias/{id}/disponibilidad  (ADMIN, BARISTA)
    // cuerpo: { "disponible": false } agota toda la categoría, true la repone.
    // reutiliza el mismo DisponibilidadRequest que el PATCH de productos (endpoint 5).
    // 200 con la categoría; 400 si falta "disponible"; 404 si la categoría no existe
    @PatchMapping("/{id}/disponibilidad")
    public CategoriaResponse cambiarDisponibilidad(@PathVariable Long id,
                                                   @Valid @RequestBody DisponibilidadRequest request) {
        log.info("PATCH /categorias/{}/disponibilidad", id);
        return categoriaService.cambiarDisponibilidad(id, request);
    }
}
