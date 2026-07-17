package ec.edu.espe.mastergateway.modulo.dto;

import ec.edu.espe.mastergateway.modulo.Modulo;
import java.util.UUID;

public record ModuloResponse(UUID id, String nombre, String descripcion, String estado) {

    public static ModuloResponse from(Modulo modulo) {
        return new ModuloResponse(modulo.getId(), modulo.getNombre(), modulo.getDescripcion(), modulo.getEstado().name());
    }
}
