package es.safareyes.coffestation.Repositorio;

import es.safareyes.coffestation.Modelo.Alergeno;
import es.safareyes.coffestation.Modelo.Cliente;
import es.safareyes.coffestation.Modelo.Pedido;
import es.safareyes.coffestation.Modelo.Producto;
import es.safareyes.coffestation.Modelo.ProductoPedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import es.safareyes.coffestation.dto.response.CategoriaResponse;
import es.safareyes.coffestation.dto.response.ProductoTopView;
import es.safareyes.coffestation.dto.response.VentasDiaDto;
import es.safareyes.coffestation.dto.response.VentasHoraView;
import es.safareyes.coffestation.Modelo.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ejecuta cada consulta contra la BD local e imprime lo que devuelve.
@SpringBootTest
public class Coffestationconsultas {

    @Autowired
    private Productorepo productorepo;

    @Autowired
    private Categoriarepo categoriarepo;

    @Autowired
    private Pedidorepo pedidorepo;

    @Autowired
    private Clienterepo clienterepo;

    // GET /api/v1/productos
    @Test
    void buscarProductosPorNombre() {

        Page<Producto> pagina =
                productorepo.findByNombreContainingIgnoreCaseAndActivoTrue("café", PageRequest.of(0, 10));

        System.out.println("Productos encontrados: " + pagina.getTotalElements());

        for (Producto producto : pagina.getContent()) {
            System.out.println("ID: " + producto.getId());
            System.out.println("Nombre: " + producto.getNombre());
            System.out.println("Precio: " + producto.getPrecio());
        }
    }

    // GET /api/v1/productos/{id}
    @Test
    void buscarProductoConAlergenos() {

        Optional<Producto> producto = productorepo.findWithAlergenosById(9L);

        if (producto.isPresent()) {
            System.out.println("Producto: " + producto.get().getNombre());
            for (Alergeno alergeno : producto.get().getAlergenos()) {
                System.out.println("Alérgeno: " + alergeno.getNombre());


            }

        } else {
            System.out.println("No existe el producto");
        }
    }

    // GET /api/v1/categorias
    @Test
    void buscarCategoriasConProductos() {

        // ahora la consulta ya devuelve el dto directamente
        List<CategoriaResponse> categorias = categoriarepo.obtenerCategoriasConProductos();

        for (CategoriaResponse categoria : categorias) {
            System.out.println("Categoría: " + categoria.nombre() + " - Productos activos: " + categoria.productosActivos());
        }
    }


    // GET /api/v1/pedidos/{id}
    @Test
    void buscarPedidoConLineas() {

        Optional<Pedido> pedido = pedidorepo.obtenerPedidoConLineas(10L);

        if (pedido.isPresent()) {
            System.out.println("Pedido: " + pedido.get().getId() + " - Turno: " + pedido.get().getNumeroTurno());
            for (ProductoPedido linea : pedido.get().getLineas()) {
                System.out.println("Producto: " + linea.getProducto().getNombre() + " x " + linea.getCantidad());
            }

        } else {
            System.out.println("No existe el pedido");
        }
    }

    // GET /api/v1/pedidos
    @Test
    void buscarPedidosEntreFechas() {

        Page<Pedido> pagina = pedidorepo.findByFechaHoraBetween(
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 2, 23, 59),
                PageRequest.of(0, 10));
        System.out.println("Pedidos encontrados: " + pagina.getTotalElements());

        for (Pedido pedido : pagina.getContent()) {
            System.out.println("ID: " + pedido.getId() + " - Fecha: " + pedido.getFechaHora());
        }
    }


    // GET /api/v1/informes/ventas-dia?fecha=
    @Test
    void buscarVentasDelDia() {

        LocalDate dia = LocalDate.of(2026, 10, 1);
        // el "select new" ya devuelve el resumen del día hecho (solo pedidos ENTREGADO, RN-10)
        VentasDiaDto ventas = pedidorepo.obtenerVentasDia(EstadoPedido.ENTREGADO,
                dia.atStartOfDay(), dia.atTime(23, 59, 59));

        System.out.println("Pedidos: " + ventas.numeroPedidos());
        System.out.println("Total: " + ventas.total());
        System.out.println("Base: " + ventas.base());
        System.out.println("IVA: " + ventas.iva());

        // y el desglose por horas (consulta nativa con extract)
        for (VentasHoraView hora : pedidorepo.obtenerVentasPorHora(dia.atStartOfDay(), dia.atTime(23, 59, 59))) {
            System.out.println("Hora " + hora.getHora() + ": " + hora.getPedidos() + " pedidos, " + hora.getTotal() + " €");
        }
    }

    // GET /api/v1/informes/productos-top?desde=&hasta=&limite=
    @Test
    void buscarProductosMasVendidos() {


        // cada fila es un ProductoTopView (proyección por interfaz)
        List<ProductoTopView> top = productorepo.obtenerProductosTop(
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 3, 23, 59),
                5);

        for (ProductoTopView fila : top) {
            System.out.println("Producto: " + fila.getProducto() + " - Unidades: " + fila.getUnidades()
                    + " - Importe: " + fila.getImporte());
        }
    }

    // EXTRA
    @Test
    void buscarProductosAgotados() {

        List<Producto> agotados = productorepo.findByDisponibleFalse();
        System.out.println("Productos agotados: " + agotados.size());
        for (Producto producto : agotados) {
            System.out.println("Nombre: " + producto.getNombre());
        }
    }


    // EXTRA
    @Test
    void buscarClientePorEmail() {

        Optional<Cliente> cliente = clienterepo.findByEmail("ana.lopez@correo.es");

        if (cliente.isPresent()) {
            System.out.println("Cliente: " + cliente.get().getNombre() + " - DNI: " + cliente.get().getDni());
        } else {
            System.out.println("No existe el cliente");
        }
    }
}
