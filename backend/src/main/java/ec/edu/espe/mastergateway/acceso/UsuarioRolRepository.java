package ec.edu.espe.mastergateway.acceso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UUID> {

    /** Roles que tiene un usuario: se usa en /api/auth/login para listarlos. */
    List<UsuarioRol> findByUsuarioId(UUID usuarioId);

    /** Se usa en /api/auth/select-role para validar que el usuario sí posee ese rol. */
    Optional<UsuarioRol> findByUsuarioIdAndRolId(UUID usuarioId, UUID rolId);

    boolean existsByUsuarioIdAndRolId(UUID usuarioId, UUID rolId);

    /** Usada por RolService para bloquear DELETE /api/roles/{id} si el rol sigue en uso. */
    boolean existsByRolId(UUID rolId);

    /**
     * DELETE /api/roles/{id}/users/{userId} exige eliminación física en la tabla
     * pivote (a diferencia del resto de entidades). @Modifying con JPQL bypasa
     * el @SQLDelete de la entidad, que solo intercepta el remove() del ciclo de
     * vida de Hibernate, no un DELETE en bloque.
     */
    @Modifying
    @Query("delete from UsuarioRol ur where ur.usuario.id = :usuarioId and ur.rol.id = :rolId")
    void hardDeleteByUsuarioIdAndRolId(@Param("usuarioId") UUID usuarioId, @Param("rolId") UUID rolId);
}
