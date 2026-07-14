package ec.edu.espe.mastergateway.auth.dto;

import java.util.List;

public record LoginResponse(String tempToken, List<RolResumenDto> roles) {
}
