package es.safareyes.coffestation.controller;

import es.safareyes.coffestation.dto.request.CuponRequest;
import es.safareyes.coffestation.dto.response.CuponResponse;
import es.safareyes.coffestation.service.CuponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

// endpoints de los cupones: de momento el 13 (el 14, validar, va luego)
//
// roles (fase 4):
//   POST /cupones -> ADMIN
@Slf4j
@RestController
@RequestMapping("/api/v1/cupones")
@RequiredArgsConstructor
public class CuponController {

    private final CuponService cuponService;

    // 13. POST /api/v1/cupones  (ADMIN)
    // 201 + cabecera Location; 400 datos mal (o código con formato raro);
    // 404 cliente no existe; 409 código repetido; 422 fechas al revés o porcentaje > 100
    @PostMapping
    public ResponseEntity<CuponResponse> crear(@Valid @RequestBody CuponRequest request) {
        log.info("POST /cupones");
        CuponResponse creado = cuponService.crear(request);

        // igual que en el POST de productos: url de esta petición + el id nuevo
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }
}
