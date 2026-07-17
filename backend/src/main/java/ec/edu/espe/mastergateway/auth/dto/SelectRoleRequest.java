package ec.edu.espe.mastergateway.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectRoleRequest(
        @NotBlank String tempToken,
        @NotNull UUID roleId) {
}
