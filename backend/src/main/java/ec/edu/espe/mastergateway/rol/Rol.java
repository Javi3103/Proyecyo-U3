package ec.edu.espe.mastergateway.rol;

import ec.edu.espe.mastergateway.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "rol")
@SQLRestriction("estado = 'ACTIVO'")
@SQLDelete(sql = "UPDATE rol SET estado = 'INACTIVO' WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
public class Rol extends Auditable {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;
}
