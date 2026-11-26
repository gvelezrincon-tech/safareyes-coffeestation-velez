package es.safareyes.coffestation.dto.response;

import java.math.BigDecimal;

// resultado de la consulta del informe GET /informes/ventas-dia (lo crea el "select new" de Pedidorepo).
// los sum vienen a null si ese día no hubo pedidos entregados: el servicio los pasa a 0
public record VentasDiaDto(
        Long numeroPedidos,
        BigDecimal total,
        BigDecimal base,
        BigDecimal iva
) {
}
