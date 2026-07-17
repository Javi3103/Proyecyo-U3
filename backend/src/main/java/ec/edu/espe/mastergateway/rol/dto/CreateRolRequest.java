package ec.edu.espe.mastergateway.rol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRolRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 255) String descripcion) {
}
