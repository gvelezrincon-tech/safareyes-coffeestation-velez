package es.safareyes.coffestation.Repositorio;

import es.safareyes.coffestation.Modelo.Producto;
import es.safareyes.coffestation.dto.response.ProductoTopView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

// JpaSpecificationExecutor nos regala el findAll(Specification, Pageable) que usa la carta con filtros
public interface Productorepo extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    // GET /api/v1/productos (endpoint 1)
    // carta paginada con los filtros de ProductoSpecification.
    //
    // este método ya existe (viene de JpaSpecificationExecutor), lo repetimos solo para ponerle
    // el @EntityGraph. sin él, al sacar 10 productos Hibernate haría 1 consulta para los productos
    // y luego 1 más por cada producto para buscar su categoría (el famoso problema N+1).
    // con @EntityGraph se trae la categoría en el mismo select con un join.
    @Override
    @EntityGraph(attributePaths = {"categoria"})
    Page<Producto> findAll(Specification<Producto> spec, Pageable pageable);

    // GET /api/v1/productos
    // carta paginada: productos activos cuyo nombre contiene un texto (?page=&size=&sort=)
    Page<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String texto, Pageable pageable);

    // GET /api/v1/productos/{id} (endpoint 2)
    // detalle de un producto con sus alérgenos y su categoría, todo en una sola consulta
    @EntityGraph(attributePaths = {"alergenos", "categoria"})
    Optional<Producto> findWithAlergenosById(Long id);

    // GET /api/v1/informes/productos-top?desde=&hasta=&limite= (endpoint 16, consulta Q3 nativa + Q5 interfaz)
    // ranking de productos más vendidos entre dos fechas, solo de pedidos ENTREGADO (RN-10).
    // el importe sale del precio guardado en cada línea (RN-03), no del precio de hoy.
    // sum de un bigint en postgres da numeric, por eso el cast a bigint (para que encaje en un Long).
    // los "as productoId", "as producto"... tienen que llamarse igual que los getters de ProductoTopView
    @Query(value = "select pr.id as productoId, pr.nombre as producto, " +
            "cast(sum(pp.cantidad) as bigint) as unidades, " +
            "sum(pp.cantidad * pp.precio_unitario) as importe " +
            "from coffestation.producto_pedido pp " +
            "join coffestation.producto pr on pr.id = pp.producto_id " +
            "join coffestation.pedido pe on pe.id = pp.pedido_id " +
            "where pe.estado = 'ENTREGADO' and pe.fecha_hora between :desde and :hasta " +
            "group by pr.id, pr.nombre " +
            "order by unidades desc, importe desc limit :limite", nativeQuery = true)
    List<ProductoTopView> obtenerProductosTop(@Param("desde") LocalDateTime desde,
                                              @Param("hasta") LocalDateTime hasta,
                                              @Param("limite") int limite);

    // EXTRA: productos agotados
    List<Producto> findByDisponibleFalse();

    // PATCH /api/v1/categorias/{id}/disponibilidad (endpoint 8, consulta Q4)
    // agota o repone de golpe todos los productos activos de una categoría con un solo update,
    // en vez de ir cargando y cambiando producto a producto. devuelve cuántas filas ha cambiado.
    //
    // @Modifying: le dice a Spring que esto no es un select sino un update.
    // flushAutomatically = true: antes del update guarda en la bd los cambios pendientes
    //   (en el servicio ya hemos cambiado la categoría y no queremos perderlo).
    // clearAutomatically = true: después del update vacía la caché de Hibernate, porque el
    //   update va directo a la bd y si no Hibernate seguiría viendo los productos con el valor viejo
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Producto p set p.disponible = :valor " +
            "where p.categoria.id = :categoriaId and p.activo = true")
    int cambiarDisponibilidadDeCategoria(@Param("categoriaId") Long categoriaId,
                                         @Param("valor") Boolean valor);

}
