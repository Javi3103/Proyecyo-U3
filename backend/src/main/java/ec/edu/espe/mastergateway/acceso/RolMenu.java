package ec.edu.espe.mastergateway.acceso;

import ec.edu.espe.mastergateway.common.audit.Auditable;
import ec.edu.espe.mastergateway.menu.Menu;
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

/**
 * Tabla pivote M:N Rol-Menú: granularidad fina dentro de un módulo (OE2 exige
 * que la estructura de menús esté "asociada directamente a roles", no solo a
 * través del módulo).
 */
@Entity
@Table(name = "rol_menu", uniqueConstraints = @UniqueConstraint(name = "uk_rol_menu", columnNames = {"rol_id", "menu_id"}))
@SQLRestriction("estado = 'ACTIVO'")
@SQLDelete(sql = "UPDATE rol_menu SET estado = 'INACTIVO' WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
public class RolMenu extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    public RolMenu(Rol rol, Menu menu) {
        this.rol = rol;
        this.menu = menu;
    }
}
