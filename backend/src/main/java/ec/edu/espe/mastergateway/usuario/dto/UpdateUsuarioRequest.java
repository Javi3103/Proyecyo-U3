package ec.edu.espe.mastergateway.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUsuarioRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 150) String nombreCompleto) {
}
