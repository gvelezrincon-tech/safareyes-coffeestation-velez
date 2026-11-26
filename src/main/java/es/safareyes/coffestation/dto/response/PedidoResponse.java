package es.safareyes.coffestation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// lo que devuelve la API de un pedido.
// cliente va aplanado (su nombre). subtotal, descuento y total no están en la tabla pedido:
//   subtotal  = suma de las líneas (cantidad * precio unitario)
//   total     = base + iva
//   descuento = subtotal - total (el importe en sí se guarda en pedido_cupones)
public record PedidoResponse(
        Long id,
        Long numeroTurno,
        LocalDateTime fechaHora,
        // el enum EstadoPedido sale como texto ("PENDIENTE"); MapStruct lo convierte solo
        String estado,
        String cliente,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal base,
        BigDecimal iva,
        BigDecimal total,
        List<LineaPedidoResponse> lineas
) {
}






