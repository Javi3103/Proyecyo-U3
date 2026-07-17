package ec.edu.espe.mastergateway.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUsuarioRequest(
        @NotBlank @Email @Size(max = 150) String email,

        /** Shift-Left: al menos 8 caracteres, con letras y números. */
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, incluyendo letras y números.")
        String password,

        @NotBlank @Size(max = 150) String nombreCompleto) {
}
