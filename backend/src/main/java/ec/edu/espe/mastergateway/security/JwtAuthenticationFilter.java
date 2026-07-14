package ec.edu.espe.mastergateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Middleware Zero Trust: valida firma y expiración del Access Token en cada
 * request y reconstruye el contexto de seguridad. Solo acepta tokens con
 * tipo=ACCESS; TEMP y REFRESH quedan reservados a sus endpoints dedicados.
 * Cualquier fallo de validación simplemente deja la request sin autenticar
 * (no lanza), y es la regla de autorización de SecurityConfig la que
 * responde 401 en los endpoints protegidos.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                autenticarSiEsAccessToken(token, request);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private void autenticarSiEsAccessToken(String token, HttpServletRequest request) {
        Claims claims = jwtService.parseClaims(token).getPayload();
        if (!JwtService.TIPO_ACCESS.equals(claims.get(JwtService.CLAIM_TIPO, String.class))) {
            return;
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UUID rolId = UUID.fromString(claims.get(JwtService.CLAIM_ROL_ID, String.class));
        String rolNombre = claims.get(JwtService.CLAIM_ROL_NOMBRE, String.class);
        List<String> permisos = claims.get(JwtService.CLAIM_PERMISOS, List.class);
        AuthenticatedUser principal = new AuthenticatedUser(userId, rolId, rolNombre, permisos == null ? List.of() : permisos);

        List<GrantedAuthority> authorities = principal.permisos().stream()
                .map(permiso -> (GrantedAuthority) new SimpleGrantedAuthority(permiso))
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
