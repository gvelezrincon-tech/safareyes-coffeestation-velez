package es.safareyes.coffestation.controller;

import es.safareyes.coffestation.Modelo.EstadoPedido;
import es.safareyes.coffestation.dto.request.EstadoPedidoRequest;
import es.safareyes.coffestation.dto.request.PedidoRequest;
import es.safareyes.coffestation.dto.response.PedidoResponse;
import es.safareyes.coffestation.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

// endpoints de los pedidos: 9, 10, 11 y 12
//
// roles (fase 4):
//   POST  -> BARISTA, CLIENTE (el cliente solo para él mismo)
//   GET   -> ADMIN, BARISTA y CLIENTE (solo los suyos)
//   PATCH -> ADMIN, BARISTA
@Slf4j
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    // 9. POST /api/v1/pedidos  (BARISTA, CLIENTE)
    // cuerpo: { "clienteId": 3, "lineas": [ { "productoId": 2, "cantidad": 2 } ],
    //           "codigoCupon": "OTONO15", "dniSocio": "12345678Z" }   (clienteId, codigoCupon y dniSocio opcionales)
    // 201 + Location con el pedido, su turno y el bloque "integracion" (qué ha dicho FitZone);
    // 400 datos mal (sin líneas, más de 15, cantidad fuera de 1-20, DNI mal escrito);
    // 404 cliente o producto que no existe;
    // 422 producto agotado o de baja, más de 20 unidades de un producto o cupón que no vale
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest request) {
        log.info("POST /pedidos");
        PedidoResponse creado = pedidoService.crear(request);

        // igual que en el POST de productos: url de esta petición + el id nuevo
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    // 10. GET /api/v1/pedidos/{id}
    // 200 con el pedido y sus líneas; 404 si no existe; 400 si el id no es un número
    @GetMapping("/{id}")
    public PedidoResponse obtenerPorId(@PathVariable Long id) {
        log.info("GET /pedidos/{}", id);
        return pedidoService.obtenerPorId(id);
    }

    // 11. GET /api/v1/pedidos?estado=PENDIENTE&fecha=2026-11-19&page=0&size=10&sort=fechaHora,desc
    // pedidos paginados (10 por página, los más nuevos primero si no se dice otra cosa).
    // estado y fecha son opcionales y se pueden juntar; si no llega ninguno, salen todos.
    // estado: PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO o CANCELADO (Spring pasa el texto al enum solo)
    // fecha: formato yyyy-MM-dd.
    // 400 si el estado no existe (?estado=frio) o la fecha está mal escrita (?fecha=ayer)
    @GetMapping
    public Page<PedidoResponse> listar(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @PageableDefault(size = 10, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("GET /pedidos");
        return pedidoService.listar(estado, fecha, pageable);
    }

    // 12. PATCH /api/v1/pedidos/{id}/estado  (ADMIN, BARISTA)
    // cuerpo: { "estado": "EN_PREPARACION" }
    // 200 con el pedido; 400 si el estado falta o no existe; 404 si el pedido no existe;
    // 422 si el salto no está permitido por la RN-08 (p. ej. de ENTREGADO a PENDIENTE)
    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id,
                                        @Valid @RequestBody EstadoPedidoRequest request) {
        log.info("PATCH /pedidos/{}/estado", id);
        return pedidoService.cambiarEstado(id, request);
    }
}
