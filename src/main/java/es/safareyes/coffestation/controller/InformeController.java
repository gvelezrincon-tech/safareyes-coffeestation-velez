package es.safareyes.coffestation.controller;

import es.safareyes.coffestation.dto.response.ProductoTopDto;
import es.safareyes.coffestation.dto.response.VentasDiaResponse;
import es.safareyes.coffestation.service.InformeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// endpoints de los informes: 15 y 16
//
// roles (fase 4):
//   GET -> ADMIN
@Slf4j
@RestController
@RequestMapping("/api/v1/informes")
@RequiredArgsConstructor
public class InformeController {

    private final InformeService informeService;

    // 15. GET /api/v1/informes/ventas-dia?fecha=2026-10-03  (ADMIN)
    // nº de pedidos, total, base e iva del día, y el desglose por horas. solo pedidos ENTREGADO (RN-10).
    // fecha en formato yyyy-MM-dd
    // 200 siempre (un día sin ventas sale a 0); 400 si falta la fecha o está mal escrita
    @GetMapping("/ventas-dia")
    public VentasDiaResponse ventasDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        log.info("GET /informes/ventas-dia?fecha={}", fecha);
        return informeService.ventasDia(fecha);
    }

    // 16. GET /api/v1/informes/productos-top?desde=2026-10-01&hasta=2026-10-31&limite=5  (ADMIN)
    // ranking de productos más vendidos entre dos fechas (las dos incluidas). solo pedidos ENTREGADO.
    // limite es opcional: 5 si no llega, entre 1 y 50.
    // 200 con la lista (vacía si no hay ventas); 400 si falta una fecha, está mal escrita
    // o el límite se sale de 1-50; 422 si desde es después de hasta
    @GetMapping("/productos-top")
    public List<ProductoTopDto> productosTop(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "El límite mínimo es 1")
            @Max(value = 50, message = "El límite máximo es 50") int limite) {
        log.info("GET /informes/productos-top?desde={}&hasta={}&limite={}", desde, hasta, limite);
        return informeService.productosTop(desde, hasta, limite);
    }
}
