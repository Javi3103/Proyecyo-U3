package ec.edu.espe.mastergateway.auth;

import ec.edu.espe.mastergateway.auth.dto.LoginRequest;
import ec.edu.espe.mastergateway.auth.dto.LoginResponse;
import ec.edu.espe.mastergateway.auth.dto.RefreshTokenRequest;
import ec.edu.espe.mastergateway.auth.dto.RefreshTokenResponse;
import ec.edu.espe.mastergateway.auth.dto.SelectRoleRequest;
import ec.edu.espe.mastergateway.auth.dto.SelectRoleResponse;
import ec.edu.espe.mastergateway.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/select-role")
    public SelectRoleResponse selectRole(@Valid @RequestBody SelectRoleRequest request) {
        return authService.selectRole(request);
    }

    @PostMapping("/refresh-token")
    public RefreshTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refrescar(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        authService.logout(principal);
        return ResponseEntity.noContent().build();
    }
}
