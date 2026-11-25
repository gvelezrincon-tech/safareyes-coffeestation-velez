package es.safareyes.coffestation.Repositorio;

import es.safareyes.coffestation.Modelo.Cupones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository

public interface Cuponesrepo extends JpaRepository<Cupones, Long> {

    // POST /api/v1/cupones (endpoint 13)
    // ¿ya hay un cupón con este código? sin mirar mayúsculas: "cafe1euro" choca con "CAFE1EURO"
    boolean existsByCodigoIgnoreCase(String codigo);

    // POST /api/v1/cupones/validar (endpoint 14)
    // busca el cupón por su código sin mirar mayúsculas ("bienvenida10" encuentra "BIENVENIDA10")
    Optional<Cupones> findByCodigoIgnoreCase(String codigo);

}
