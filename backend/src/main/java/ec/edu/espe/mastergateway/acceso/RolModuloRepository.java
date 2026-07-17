package ec.edu.espe.mastergateway.acceso;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolModuloRepository extends JpaRepository<RolModulo, UUID> {

    List<RolModulo> findByRolId(UUID rolId);

    boolean existsByRolIdAndModuloId(UUID rolId, UUID moduloId);
}
