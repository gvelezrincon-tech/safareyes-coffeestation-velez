package es.safareyes.coffestation.mapper;

import es.safareyes.coffestation.Modelo.Pedido;
import es.safareyes.coffestation.Modelo.ProductoPedido;
import es.safareyes.coffestation.dto.response.PedidoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.Set;

// pasa de Pedido (entidad) a su dto.
// uses = LineaPedidoMapper: para la lista de líneas MapStruct llama a ese mapper con cada línea
@Mapper(uses = LineaPedidoMapper.class)
public interface PedidoMapper {

    // el cliente se queda solo con su nombre.
    // total, subtotal y descuento no están en la tabla, así que los calculamos:
    //   total = base + iva, subtotal = suma de las líneas, descuento = subtotal - total
    @Mapping(target = "cliente", source = "cliente.nombre")
    @Mapping(target = "total", expression = "java(pedido.getBase().add(pedido.getIva()))")
    @Mapping(target = "subtotal", expression = "java(subtotal(pedido.getLineas()))")
    @Mapping(target = "descuento", expression = "java(subtotal(pedido.getLineas()).subtract(pedido.getBase().add(pedido.getIva())))")
    @Mapping(target = "lineas", source = "lineas")
    PedidoResponse toResponse(Pedido pedido);

    // suma de cantidad * precio unitario de cada línea (RN-03: con el precio que se cobró)
    default BigDecimal subtotal(Set<ProductoPedido> lineas) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ProductoPedido linea : lineas) {
            subtotal = subtotal.add(linea.getPrecioUnitario().multiply(BigDecimal.valueOf(linea.getCantidad())));
        }
        return subtotal.setScale(2);
    }
}
