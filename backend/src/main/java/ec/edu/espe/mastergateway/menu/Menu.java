package ec.edu.espe.mastergateway.menu;

import ec.edu.espe.mastergateway.common.audit.Auditable;
import ec.edu.espe.mastergateway.modulo.Modulo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Nodo de la estructura de menú (Adjacency List). Un mismo registro puede ser
 * Menú Principal (parent == null), Submenú o Item hoja (url != null).
 *
 * No se mapea una colección de hijos (@OneToMany mappedBy = "parent"):
 * recorrer el árbol nivel por nivel vía JPA dispararía una consulta por cada
 * nodo padre (N+1). En su lugar, MenuRepository expone una consulta nativa
 * con WITH RECURSIVE que trae el árbol completo en una sola ida a la base.
 */
@Entity
@Table(name = "menu")
@SQLRestriction("estado = 'ACTIVO'")
@SQLDelete(sql = "UPDATE menu SET estado = 'INACTIVO' WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
public class Menu extends Auditable {

    @Column(nullable = false, length = 150)
    private String nombre;

    /** Solo se completa en nodos hoja (Items); null en Módulos/Submenús agrupadores. */
    @Column(length = 255)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    /** Null = Menú Principal. Con valor = Submenú o Item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Menu parent;
}
