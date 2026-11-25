package es.safareyes.coffestation.Repositorio;

import es.safareyes.coffestation.Modelo.Cupones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface Cuponesrepo extends JpaRepository<Cupones, Long> {

    // POST /api/v1/cupones (endpoint 13)
    // ¿ya hay un cupón con este código? sin mirar mayúsculas: "cafe1euro" choca con "CAFE1EURO"
    boolean existsByCodigoIgnoreCase(String codigo);

}
