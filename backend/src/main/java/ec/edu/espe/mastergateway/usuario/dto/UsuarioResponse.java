package ec.edu.espe.mastergateway.usuario.dto;

import ec.edu.espe.mastergateway.usuario.Usuario;
import java.time.Instant;
import java.util.UUID;

/** Nunca expone passwordHash: la contraseña no debe salir serializada en ningún endpoint. */
public record UsuarioResponse(
        UUID id,
        String email,
        String nombreCompleto,
        String estado,
        Instant fechaCreacion,
        Instant fechaActualizacion) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                usuario.getEstado().name(),
                usuario.getFechaCreacion(),
                usuario.getFechaActualizacion());
    }
}
