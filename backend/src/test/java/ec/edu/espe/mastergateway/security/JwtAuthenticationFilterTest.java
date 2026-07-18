package ec.edu.espe.mastergateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Este filtro es la puerta Zero Trust del backend: toda request pasa por
 * aquí antes de llegar a cualquier Controller. Confirma que solo un Access
 * Token (nunca TEMP ni REFRESH, nunca uno inválido) reconstruye el contexto
 * de seguridad, y que un fallo de validación nunca interrumpe la cadena de
 * filtros (deja la request sin autenticar; es SecurityConfig quien decide
 * si eso resulta en 401).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "Sl6Il0K6VqxYVdqVril6CsNp366UpSMH4eLBiUQ/bko=";

    private final JwtService jwtService = new JwtService(TEST_SECRET, 300_000, 900_000, 604_800_000);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinHeaderAuthorization_dejaLaRequestSinAutenticarYContinuaLaCadena() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void headerSinPrefijoBearer_esIgnorado() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenMalformado_noPropagaExcepcionYDejaLaRequestSinAutenticar() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer esto-no-es-un-jwt");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void tempToken_noAutentica() throws Exception {
        String tempToken = jwtService.generarTempToken(UUID.randomUUID());
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + tempToken);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void refreshToken_noAutentica() throws Exception {
        String refreshToken = jwtService.generarRefreshToken(UUID.randomUUID(), UUID.randomUUID());
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + refreshToken);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void accessTokenValido_reconstruyeElPrincipalConSusPermisosComoAuthorities() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        List<String> permisos = List.of("/ventas/crear-orden", "/ventas/listar");
        String accessToken = jwtService.generarAccessToken(usuarioId, rolId, "Vendedor", permisos);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + accessToken);

        filter.doFilterInternal(request, response, chain);

        var authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(usuarioId);
        assertThat(principal.rolId()).isEqualTo(rolId);
        assertThat(principal.rolNombre()).isEqualTo("Vendedor");

        List<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(authorities).containsExactlyInAnyOrderElementsOf(permisos);
        verify(chain).doFilter(request, response);
    }
}
