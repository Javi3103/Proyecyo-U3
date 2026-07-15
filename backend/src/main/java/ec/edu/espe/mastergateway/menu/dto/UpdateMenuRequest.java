package ec.edu.espe.mastergateway.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateMenuRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 255) String url,
        UUID parentId) {
}
