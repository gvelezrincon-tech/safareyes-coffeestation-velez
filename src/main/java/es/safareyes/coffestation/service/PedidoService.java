package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.EstadoPedido;
import es.safareyes.coffestation.dto.request.EstadoPedidoRequest;
import es.safareyes.coffestation.dto.request.PedidoRequest;
import es.safareyes.coffestation.dto.response.PedidoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

// lo que se puede hacer con los pedidos (endpoints 9, 10, 11 y 12)
public interface PedidoService {

    // endpoint 9: crear un pedido con sus líneas, su turno y su descuento (cupón o socio FitZone)
    PedidoResponse crear(PedidoRequest request);

    // endpoint 10: detalle de un pedido con sus líneas
    PedidoResponse obtenerPorId(Long id);

    // endpoint 11: pedidos paginados; estado y fecha son opcionales y se pueden combinar
    Page<PedidoResponse> listar(EstadoPedido estado, LocalDate fecha, Pageable pageable);

    // endpoint 12: cambiar el estado de un pedido respetando las transiciones de la RN-08
    PedidoResponse cambiarEstado(Long id, EstadoPedidoRequest request);
}
