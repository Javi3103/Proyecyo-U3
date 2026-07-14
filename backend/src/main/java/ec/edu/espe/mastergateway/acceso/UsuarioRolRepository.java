package ec.edu.espe.mastergateway.acceso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UUID> {

    /** Roles que tiene un usuario: se usa en /api/auth/login para listarlos. */
    List<UsuarioRol> findByUsuarioId(UUID usuarioId);

    /** Se usa en /api/auth/select-role para validar que el usuario sí posee ese rol. */
    Optional<UsuarioRol> findByUsuarioIdAndRolId(UUID usuarioId, UUID rolId);

    boolean existsByUsuarioIdAndRolId(UUID usuarioId, UUID rolId);
}
