package es.safareyes.coffestation.dto.response;

import java.math.BigDecimal;

// proyección por interfaz (Q5) de la consulta nativa de ventas por franja horaria.
// spring hace un objeto que cumple esta interfaz con cada fila: getHora() da la columna "hora", etc.
public interface VentasHoraView {

    Integer getHora();

    Long getPedidos();

    BigDecimal getTotal();
}
