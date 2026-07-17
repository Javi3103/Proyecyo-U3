package ec.edu.espe.mastergateway.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Estándar de campos obligatorios por entidad. fecha_creacion, fecha_actualizacion,
 * creado_por y actualizado_por son gestionados exclusivamente por AuditingEntityListener
 * (hooks @PrePersist/@PreUpdate internos de Hibernate) y no exponen setters públicos
 * fuera de este paquete, para que un controlador no pueda sobreescribirlos.
 */
@Getter
@Setter(lombok.AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter(lombok.AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @CreatedBy
    @Column(name = "creado_por", updatable = false)
    private UUID creadoPor;

    @LastModifiedBy
    @Column(name = "actualizado_por")
    private UUID actualizadoPor;

    /**
     * Único punto de entrada para inactivar (soft delete). No existe un setEstado()
     * público: cambiar el estado es una acción de negocio explícita, no un campo más
     * a actualizar por PUT.
     */
    public void inactivar() {
        this.estado = Estado.INACTIVO;
    }

    public void activar() {
        this.estado = Estado.ACTIVO;
    }

    public boolean estaActivo() {
        return this.estado == Estado.ACTIVO;
    }
}
