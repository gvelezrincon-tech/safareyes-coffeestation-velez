package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.EstadoPedido;
import es.safareyes.coffestation.Repositorio.Pedidorepo;
import es.safareyes.coffestation.Repositorio.Productorepo;
import es.safareyes.coffestation.dto.response.ProductoTopDto;
import es.safareyes.coffestation.dto.response.ProductoTopView;
import es.safareyes.coffestation.dto.response.VentasDiaDto;
import es.safareyes.coffestation.dto.response.VentasDiaResponse;
import es.safareyes.coffestation.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// lógica de los informes (endpoints 15 y 16)
@Slf4j
@Service
@RequiredArgsConstructor
public class InformeServiceImpl implements InformeService {

    private final Pedidorepo pedidorepo;
    private final Productorepo productorepo;


    // endpoint 15 - GET /api/v1/informes/ventas-dia?fecha=2026-10-03
    // url: http://localhost:8081/api/v1/informes/ventas-dia?fecha=2026-10-03
    // RN-10: solo pedidos ENTREGADO. un día sin ventas sale con todo a 0 (no es un error)
    @Override
    @Transactional(readOnly = true)
    public VentasDiaResponse ventasDia(LocalDate fecha) {
        log.info("Informe de ventas del día {}", fecha);

        // el día entero: desde las 00:00 hasta justo antes de las 00:00 del día siguiente
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay().minusNanos(1);

        VentasDiaDto ventas = pedidorepo.obtenerVentasDia(EstadoPedido.ENTREGADO, inicio, fin);

        List<VentasDiaResponse.VentasHora> porHora = pedidorepo.obtenerVentasPorHora(inicio, fin).stream()
                .map(h -> new VentasDiaResponse.VentasHora(h.getHora(), h.getPedidos(), h.getTotal()))
                .toList();

        return new VentasDiaResponse(fecha, ventas.numeroPedidos(),
                ceroSiNull(ventas.total()), ceroSiNull(ventas.base()), ceroSiNull(ventas.iva()), porHora);
    }


    // endpoint 16 - GET /api/v1/informes/productos-top?desde=2026-10-01&hasta=2026-10-31&limite=5
    // url: http://localhost:8081/api/v1/informes/productos-top?desde=2026-10-01&hasta=2026-10-31&limite=5
    // RN-10: solo pedidos ENTREGADO. desde y hasta van incluidos (hasta = hasta las 23:59:59 de ese día).
    // 422 si desde es después de hasta (el límite 1-50 ya lo comprueba el controlador)
    @Override
    @Transactional(readOnly = true)
    public List<ProductoTopDto> productosTop(LocalDate desde, LocalDate hasta, int limite) {
        log.info("Productos más vendidos del {} al {} (top {})", desde, hasta, limite);

        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha 'desde' (" + desde + ") no puede ser posterior a 'hasta' (" + hasta + ")");
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay().minusNanos(1);
        List<ProductoTopView> filas = productorepo.obtenerProductosTop(inicio, fin, limite);

        // de la proyección al dto, poniendo la posición en el ranking (1, 2, 3...)
        List<ProductoTopDto> ranking = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            ProductoTopView fila = filas.get(i);
            ranking.add(new ProductoTopDto(i + 1, fila.getProductoId(), fila.getProducto(),
                    fila.getUnidades(), fila.getImporte()));
        }
        return ranking;
    }


    // los sum de sql dan null si no hay ninguna fila: mejor enseñar 0.00
    private BigDecimal ceroSiNull(BigDecimal valor) {
        return (valor != null) ? valor : new BigDecimal("0.00");
    }
}
