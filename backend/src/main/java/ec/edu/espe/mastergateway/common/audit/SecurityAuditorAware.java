package ec.edu.espe.mastergateway.common.audit;

import ec.edu.espe.mastergateway.security.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resuelve el UUID del usuario autenticado (extraído del JWT por el filtro de
 * seguridad) para poblar creado_por / actualizado_por. Sin sesión autenticada
 * (ej. auto-registro), Spring Data deja esos campos en null.
 */
public class SecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user.userId());
        }
        return Optional.empty();
    }
}
