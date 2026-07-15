package ec.edu.espe.mastergateway.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateMenuRequest(
        @NotBlank @Size(max = 150) String nombre,

        /** Solo debe completarse en nodos hoja (Items); null en Módulos/Submenús agrupadores. */
        @Size(max = 255) String url,

        @NotNull UUID moduloId,

        /** Null = Menú Principal. Con valor = Submenú o Item. */
        UUID parentId) {
}
