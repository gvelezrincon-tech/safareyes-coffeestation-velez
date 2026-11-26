package es.safareyes.coffestation.service;

import es.safareyes.coffestation.Modelo.Categoria;
import es.safareyes.coffestation.Repositorio.Categoriarepo;
import es.safareyes.coffestation.Repositorio.Productorepo;
import es.safareyes.coffestation.dto.request.DisponibilidadRequest;
import es.safareyes.coffestation.dto.response.CategoriaResponse;
import es.safareyes.coffestation.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// lógica de las categorías
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final Categoriarepo categoriarepo;
    private final Productorepo productorepo;


    // endpoint 7 - GET /api/v1/categorias
    // url: http://localhost:8081/api/v1/categorias
    // aquí no hace falta mapper: la consulta ya devuelve el dto hecho (select new)
    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        log.info("Listado de categorías");
        return categoriarepo.obtenerCategoriasConProductos();
    }


    // endpoint 8 - PATCH /api/v1/categorias/{id}/disponibilidad
    // url: http://localhost:8081/api/v1/categorias/3/disponibilidad
    //
    // RF-03: el barista marca toda la bollería como agotada de una vez.
    // se cambia la categoría y, con un solo update (Q4), todos sus productos activos.
    // los de baja no se tocan: no salen en la carta, así que da igual si están agotados o no
    @Override
    @Transactional
    public CategoriaResponse cambiarDisponibilidad(Long id, DisponibilidadRequest request) {
        log.info("Categoría {} -> disponible = {}", id, request.disponible());

        Categoria categoria = categoriarepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría con id " + id));

        // ojo con el orden: primero cambiamos la categoría y luego lanzamos el update.
        // el update tiene flushAutomatically, así que guarda antes este cambio en la bd;
        // si lo hiciéramos al revés, el clearAutomatically del update "soltaría" la categoría
        // y el cambio de después no se guardaría
        categoria.setDisponible(request.disponible());
        int productosCambiados = productorepo.cambiarDisponibilidadDeCategoria(id, request.disponible());
        log.info("Categoría {}: {} productos marcados como disponible = {}",
                id, productosCambiados, request.disponible());

        // el update solo toca productos activos, así que lo que devuelve
        // es justo el nº de productos activos de la categoría
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(),
                categoria.getDisponible(), (long) productosCambiados);
    }
}
