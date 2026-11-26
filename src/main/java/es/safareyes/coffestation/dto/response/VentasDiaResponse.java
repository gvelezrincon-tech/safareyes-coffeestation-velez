package es.safareyes.coffestation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// lo que devuelve GET /informes/ventas-dia?fecha=
// ejemplo: { "fecha": "2026-10-03", "numeroPedidos": 2, "total": 7.40, "base": 6.73, "iva": 0.67,
//            "porHora": [ { "hora": 8, "pedidos": 2, "total": 7.40 } ] }
public record VentasDiaResponse(
        LocalDate fecha,
        Long numeroPedidos,
        BigDecimal total,
        BigDecimal base,
        BigDecimal iva,
        List<VentasHora> porHora
) {

    // una franja horaria: de 8:00 a 8:59 es la hora 8
    public record VentasHora(
            Integer hora,
            Long pedidos,
            BigDecimal total
    ) {
    }
}
