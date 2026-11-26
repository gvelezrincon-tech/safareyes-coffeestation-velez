package es.safareyes.coffestation.dto.response;

import java.math.BigDecimal;

// una fila del informe GET /informes/productos-top
// ejemplo: { "posicion": 1, "productoId": 2, "producto": "Café con leche", "unidadesVendidas": 14, "importe": 21.00 }
public record ProductoTopDto(
        int posicion,
        Long productoId,
        String producto,
        Long unidadesVendidas,
        BigDecimal importe
) {
}
