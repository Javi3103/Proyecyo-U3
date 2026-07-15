package ec.edu.espe.mastergateway.rol.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AsignarMenuRequest(@NotNull UUID menuId) {
}
