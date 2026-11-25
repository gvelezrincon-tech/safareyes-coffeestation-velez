package es.safareyes.coffestation.Repositorio;

import es.safareyes.coffestation.Modelo.EstadoPedido;
import es.safareyes.coffestation.Modelo.PedidoCupones;
import es.safareyes.coffestation.Modelo.PedidoCuponesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface PedidoCuponesrepo extends JpaRepository<PedidoCupones, PedidoCuponesId> {

    // POST /api/v1/cupones/validar (endpoint 14)
    // cuántas veces se ha usado un cupón: sus filas en pedido_cupones quitando las de pedidos
    // en un estado concreto (le pasamos CANCELADO: RN-08, cancelar un pedido devuelve el uso).
    // Spring lo saca del nombre: count + cupon.id = ? + pedido.estado <> ?
    long countByCuponIdAndPedidoEstadoNot(Long cuponId, EstadoPedido estado);

}
