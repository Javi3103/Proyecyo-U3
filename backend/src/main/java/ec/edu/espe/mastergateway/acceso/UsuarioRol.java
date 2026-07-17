package ec.edu.espe.mastergateway.acceso;

import ec.edu.espe.mastergateway.common.audit.Auditable;
import ec.edu.espe.mastergateway.rol.Rol;
import ec.edu.espe.mastergateway.usuario.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Tabla pivote M:N Usuario-Rol. No es una tabla "tonta": hereda auditoría
 * (fecha_creacion/estado) para saber cuándo se le otorgó o revocó un rol a
 * un usuario específico (nota 3 del documento de requisitos).
 */
@Entity
@Table(name = "usuario_rol", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_rol", columnNames = {"usuario_id", "rol_id"}))
@SQLRestriction("estado = 'ACTIVO'")
@SQLDelete(sql = "UPDATE usuario_rol SET estado = 'INACTIVO' WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioRol extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    public UsuarioRol(Usuario usuario, Rol rol) {
        this.usuario = usuario;
        this.rol = rol;
    }
}
