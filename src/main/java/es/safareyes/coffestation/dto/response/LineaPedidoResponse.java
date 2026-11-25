package es.safareyes.coffestation.dto.response;

import java.math.BigDecimal;

// una línea de un pedido tal como la ve el cliente.
// precioUnitario es el precio que tenía el producto al hacer el pedido (RN-03)
public record LineaPedidoResponse(
        Long productoId,
        String producto,
        Long cantidad,
        BigDecimal precioUnitario
) {
}








