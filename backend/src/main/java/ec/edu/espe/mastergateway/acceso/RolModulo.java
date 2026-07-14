package ec.edu.espe.mastergateway.acceso;

import ec.edu.espe.mastergateway.common.audit.Auditable;
import ec.edu.espe.mastergateway.modulo.Modulo;
import ec.edu.espe.mastergateway.rol.Rol;
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

/** Tabla pivote M:N Rol-Módulo: qué módulos administrativos ve cada rol. */
@Entity
@Table(name = "rol_modulo", uniqueConstraints = @UniqueConstraint(name = "uk_rol_modulo", columnNames = {"rol_id", "modulo_id"}))
@SQLRestriction("estado = 'ACTIVO'")
@SQLDelete(sql = "UPDATE rol_modulo SET estado = 'INACTIVO' WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
public class RolModulo extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    public RolModulo(Rol rol, Modulo modulo) {
        this.rol = rol;
        this.modulo = modulo;
    }
}
