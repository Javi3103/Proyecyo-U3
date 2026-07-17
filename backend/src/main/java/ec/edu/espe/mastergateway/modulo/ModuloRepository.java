package ec.edu.espe.mastergateway.modulo;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuloRepository extends JpaRepository<Modulo, UUID> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, UUID id);
}
