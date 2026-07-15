package ec.edu.espe.mastergateway.acceso;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolMenuRepository extends JpaRepository<RolMenu, UUID> {

    List<RolMenu> findByRolId(UUID rolId);

    boolean existsByRolIdAndMenuId(UUID rolId, UUID menuId);
}
