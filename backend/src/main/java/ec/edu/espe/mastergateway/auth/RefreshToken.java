package ec.edu.espe.mastergateway.auth;

import ec.edu.espe.mastergateway.common.audit.Auditable;
import ec.edu.espe.mastergateway.rol.Rol;
import ec.edu.espe.mastergateway.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro server-side de refresh tokens (rotación + detección de reuso).
 * A diferencia del resto de entidades, NO lleva @SQLRestriction/@SQLDelete:
 * "estado" aquí significa "vigente" (ACTIVO) vs "ya canjeado o revocado"
 * (INACTIVO), y /refresh-token necesita poder ENCONTRAR registros inactivos
 * para detectar que un token ya usado está siendo reenviado (señal de robo).
 * Filtrarlos automáticamente rompería esa detección.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;
}
