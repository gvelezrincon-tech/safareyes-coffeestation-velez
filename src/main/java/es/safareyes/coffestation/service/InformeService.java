package es.safareyes.coffestation.service;

import es.safareyes.coffestation.dto.response.ProductoTopDto;
import es.safareyes.coffestation.dto.response.VentasDiaResponse;

import java.time.LocalDate;
import java.util.List;

// informes de cierre (endpoints 15 y 16). solo cuentan pedidos ENTREGADO (RN-10)
public interface InformeService {

    // endpoint 15: nº de pedidos, total, base e iva de un día, y lo mismo por franja horaria
    VentasDiaResponse ventasDia(LocalDate fecha);

    // endpoint 16: los productos más vendidos entre dos fechas (las dos incluidas)
    List<ProductoTopDto> productosTop(LocalDate desde, LocalDate hasta, int limite);
}
