package ec.edu.espe.mastergateway.auth.dto;

import java.util.List;
import java.util.UUID;

public record ValidateTokenResponse(UUID userId, UUID roleId, String role, List<String> permissions) {
}
