package es.safareyes.coffestation.Repositorio;

import es.safareyes.coffestation.Modelo.EstadoPedido;
import es.safareyes.coffestation.Modelo.Pedido;
import es.safareyes.coffestation.dto.response.VentasDiaDto;
import es.safareyes.coffestation.dto.response.VentasHoraView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface Pedidorepo extends JpaRepository<Pedido, Long> {

    // GET /api/v1/pedidos/{id} (endpoint 10)
    // detalle de un pedido con su cliente, sus líneas y sus productos en una sola consulta
    @EntityGraph(attributePaths = {"cliente", "lineas", "lineas.producto"})
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> obtenerPedidoConLineas(@Param("id") Long id);

    // GET /api/v1/pedidos?fecha= (endpoint 11)
    // listado paginado de pedidos entre dos fechas (?page=&size=&sort=).
    // aquí el @EntityGraph solo trae el cliente: si metiéramos también las líneas (una colección)
    // hibernate no podría paginar en la bd y se traería todos los pedidos a memoria
    @EntityGraph(attributePaths = {"cliente"})
    Page<Pedido> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    // GET /api/v1/pedidos sin fecha (endpoint 11)
    // todos los pedidos paginados. lo repetimos solo para ponerle el @EntityGraph del cliente
    @Override
    @EntityGraph(attributePaths = {"cliente"})
    Page<Pedido> findAll(Pageable pageable);

    // GET /api/v1/pedidos?estado= (endpoint 11)
    // pedidos paginados de un estado (p. ej. los PENDIENTE que tiene que preparar el barista)
    @EntityGraph(attributePaths = {"cliente"})
    Page<Pedido> findByEstado(EstadoPedido estado, Pageable pageable);

    // GET /api/v1/pedidos?estado=&fecha= (endpoint 11)
    // pedidos paginados de un estado y un día a la vez
    @EntityGraph(attributePaths = {"cliente"})
    Page<Pedido> findByEstadoAndFechaHoraBetween(EstadoPedido estado, LocalDateTime desde,
                                                 LocalDateTime hasta, Pageable pageable);

    // POST /api/v1/pedidos (endpoint 9, RN-09)
    // cuántos pedidos se han creado entre dos momentos. con el día de hoy da el número de turno - 1
    long countByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);

    // GET /api/v1/informes/ventas-dia?fecha= (endpoint 15, consulta Q5 con "select new")
    // nº de pedidos, total, base e iva de los pedidos de un estado entre dos momentos.
    // le pasamos ENTREGADO: los informes solo cuentan lo que se ha cobrado (RN-10).
    // con "select new" hibernate crea directamente el dto con la única fila del resultado.
    // ojo: si ese día no hay pedidos, count da 0 pero los sum dan null (lo arregla el servicio)
    @Query("select new es.safareyes.coffestation.dto.response.VentasDiaDto(" +
            "count(p), sum(p.base + p.iva), sum(p.base), sum(p.iva)) " +
            "from Pedido p where p.estado = :estado and p.fechaHora between :inicio and :fin")
    VentasDiaDto obtenerVentasDia(@Param("estado") EstadoPedido estado,
                                  @Param("inicio") LocalDateTime inicio,
                                  @Param("fin") LocalDateTime fin);

    // GET /api/v1/informes/ventas-dia?fecha= (endpoint 15, consulta Q3 nativa + Q5 interfaz)
    // las ventas de un día por franja horaria: cuántos pedidos y cuánto dinero en cada hora
    // (para ver lo de "entre las ocho y las diez la barra es un caos").
    // extract(hour ...) saca la hora de la fecha: 08:45 -> 8. solo pedidos ENTREGADO (RN-10).
    // los "as hora", "as pedidos"... tienen que llamarse igual que los getters de VentasHoraView
    @Query(value = "select cast(extract(hour from p.fecha_hora) as integer) as hora, " +
            "count(*) as pedidos, sum(p.base + p.iva) as total " +
            "from coffestation.pedido p " +
            "where p.estado = 'ENTREGADO' and p.fecha_hora between :inicio and :fin " +
            "group by extract(hour from p.fecha_hora) order by hora", nativeQuery = true)
    List<VentasHoraView> obtenerVentasPorHora(@Param("inicio") LocalDateTime inicio,
                                              @Param("fin") LocalDateTime fin);

}
