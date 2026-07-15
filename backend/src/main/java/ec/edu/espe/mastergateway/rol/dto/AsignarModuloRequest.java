package ec.edu.espe.mastergateway.rol.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AsignarModuloRequest(@NotNull UUID moduloId) {
}
