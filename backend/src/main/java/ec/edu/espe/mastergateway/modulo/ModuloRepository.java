package ec.edu.espe.mastergateway.modulo;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuloRepository extends JpaRepository<Modulo, UUID> {
}
