package ec.edu.espe.mastergateway.auth;

import ec.edu.espe.mastergateway.auth.dto.ValidateTokenRequest;
import ec.edu.espe.mastergateway.auth.dto.ValidateTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint privado que consumen los microservicios hijos (Zero Trust: no confían en el frontend). */
@RestController
@RequestMapping("/api/internals")
@RequiredArgsConstructor
public class InternalTokenController {

    private final AuthService authService;

    @PostMapping("/validate-token")
    public ValidateTokenResponse validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        return authService.validarToken(request.token());
    }
}
