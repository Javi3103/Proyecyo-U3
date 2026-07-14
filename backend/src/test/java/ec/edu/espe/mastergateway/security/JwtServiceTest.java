package ec.edu.espe.mastergateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    // Secret de prueba, no usar fuera de tests.
    private static final String TEST_SECRET = "Sl6Il0K6VqxYVdqVril6CsNp366UpSMH4eLBiUQ/bko=";

    private final JwtService jwtService = new JwtService(TEST_SECRET, 300_000, 900_000, 604_800_000);

    @Test
    void generarTempToken_soloContieneTipoTemp() {
        UUID usuarioId = UUID.randomUUID();

        String token = jwtService.generarTempToken(usuarioId);
        Claims claims = jwtService.parseClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(claims.get(JwtService.CLAIM_TIPO, String.class)).isEqualTo(JwtService.TIPO_TEMP);
        assertThat(claims.get(JwtService.CLAIM_ROL_ID)).isNull();
        assertThat(claims.get(JwtService.CLAIM_PERMISOS)).isNull();
    }

    @Test
    void generarAccessToken_incluyeSoloElRolSeleccionadoYSusPermisos() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        List<String> permisos = List.of("ventas:crear-orden", "ventas:listar-ordenes");

        String token = jwtService.generarAccessToken(usuarioId, rolId, "Vendedor", permisos);
        Claims claims = jwtService.parseClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(claims.get(JwtService.CLAIM_TIPO, String.class)).isEqualTo(JwtService.TIPO_ACCESS);
        assertThat(claims.get(JwtService.CLAIM_ROL_ID, String.class)).isEqualTo(rolId.toString());
        assertThat(claims.get(JwtService.CLAIM_ROL_NOMBRE, String.class)).isEqualTo("Vendedor");
        assertThat(claims.get(JwtService.CLAIM_PERMISOS, List.class)).containsExactlyElementsOf(permisos);
    }

    @Test
    void generarRefreshToken_expiraMuchoDespuesQueElAccessToken() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();

        String refreshToken = jwtService.generarRefreshToken(usuarioId, rolId);
        Claims claims = jwtService.parseClaims(refreshToken).getPayload();

        assertThat(claims.get(JwtService.CLAIM_TIPO, String.class)).isEqualTo(JwtService.TIPO_REFRESH);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void parseClaims_tokenExpirado_lanzaExpiredJwtException() {
        JwtService jwtServiceExpiraInstantaneo = new JwtService(TEST_SECRET, -1, -1, -1);
        UUID usuarioId = UUID.randomUUID();

        String token = jwtServiceExpiraInstantaneo.generarTempToken(usuarioId);

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseClaims_firmaConOtroSecret_esRechazado() {
        JwtService jwtServiceOtroSecret = new JwtService(
                "b3RyYS1jbGF2ZS1kaXN0aW50YS1kZS0zMi1ieXRlcyEhISEhISEhIQ==", 300_000, 900_000, 604_800_000);
        String token = jwtServiceOtroSecret.generarTempToken(UUID.randomUUID());

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
