package es.safareyes.coffestation.dto.response;

import java.math.BigDecimal;

// proyección por interfaz (Q5) del ranking de productos más vendidos (consulta nativa de Productorepo).
// spring hace un objeto que cumple esta interfaz con cada fila: getProducto() da la columna "producto", etc.
public interface ProductoTopView {

    Long getProductoId();

    String getProducto();

    Long getUnidades();

    BigDecimal getImporte();
}
