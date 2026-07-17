package ec.edu.espe.mastergateway.rol.dto;

import ec.edu.espe.mastergateway.rol.Rol;
import java.util.UUID;

public record RolResponse(UUID id, String nombre, String descripcion, String estado) {

    public static RolResponse from(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.getEstado().name());
    }
}
