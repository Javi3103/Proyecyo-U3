package ec.edu.espe.mastergateway.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.edu.espe.mastergateway.acceso.RolMenu;
import ec.edu.espe.mastergateway.acceso.RolMenuRepository;
import ec.edu.espe.mastergateway.acceso.UsuarioRol;
import ec.edu.espe.mastergateway.acceso.UsuarioRolRepository;
import ec.edu.espe.mastergateway.auth.dto.LoginRequest;
import ec.edu.espe.mastergateway.auth.dto.LoginResponse;
import ec.edu.espe.mastergateway.auth.dto.RefreshTokenRequest;
import ec.edu.espe.mastergateway.auth.dto.RefreshTokenResponse;
import ec.edu.espe.mastergateway.auth.dto.SelectRoleRequest;
import ec.edu.espe.mastergateway.auth.dto.SelectRoleResponse;
import ec.edu.espe.mastergateway.common.exception.InvalidCredentialsException;
import ec.edu.espe.mastergateway.common.exception.InvalidTokenException;
import ec.edu.espe.mastergateway.common.exception.RateLimitExceededException;
import ec.edu.espe.mastergateway.common.exception.RoleNotAssignedException;
import ec.edu.espe.mastergateway.menu.Menu;
import ec.edu.espe.mastergateway.rol.Rol;
import ec.edu.espe.mastergateway.security.AuthenticatedUser;
import ec.edu.espe.mastergateway.security.JwtService;
import ec.edu.espe.mastergateway.security.TokenHasher;
import ec.edu.espe.mastergateway.usuario.Usuario;
import ec.edu.espe.mastergateway.usuario.UsuarioRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Cubre el flujo de autenticación completo: es la superficie de mayor riesgo
 * de seguridad del sistema (Zero Trust, Least Privilege, rotación/reuso de
 * refresh tokens, rate limiting). El JwtService usado aquí es real (no un
 * mock) porque validar que los tokens emitidos realmente decodifican con los
 * claims esperados es el punto de estas pruebas, no solo que se llamó un método.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_SECRET = "Sl6Il0K6VqxYVdqVril6CsNp366UpSMH4eLBiUQ/bko=";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private RolMenuRepository rolMenuRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LoginRateLimiter rateLimiter;

    private final JwtService jwtService = new JwtService(TEST_SECRET, 300_000, 900_000, 604_800_000);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository, usuarioRolRepository, rolMenuRepository,
                refreshTokenRepository, passwordEncoder, jwtService, rateLimiter);
    }

    private static void setId(Object entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private static Usuario usuarioCon(UUID id, String email, String hash) {
        Usuario usuario = new Usuario();
        setId(usuario, id);
        usuario.setEmail(email);
        usuario.setPasswordHash(hash);
        usuario.setNombreCompleto("Usuario de prueba");
        return usuario;
    }

    private static Rol rolCon(UUID id, String nombre) {
        Rol rol = new Rol();
        setId(rol, id);
        rol.setNombre(nombre);
        return rol;
    }

    // ── login ────────────────────────────────────────────────────────────

    @Test
    void login_rateLimitExcedido_lanzaExcepcionSinConsultarCredenciales() {
        when(rateLimiter.permitirIntento("ana@espe.edu.ec")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@espe.edu.ec", "cualquier1")))
                .isInstanceOf(RateLimitExceededException.class);

        verify(usuarioRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_normalizaEmailAntesDeConsultarElRateLimitYElRepositorio() {
        when(rateLimiter.permitirIntento("ana@espe.edu.ec")).thenReturn(true);
        when(usuarioRepository.findByEmail("ana@espe.edu.ec")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("  Ana@ESPE.edu.ec ", "cualquier1")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(rateLimiter).permitirIntento("ana@espe.edu.ec");
        verify(usuarioRepository).findByEmail("ana@espe.edu.ec");
    }

    @Test
    void login_emailInexistente_usaHashDummyParaNoRevelarCualCampoFallo() {
        when(rateLimiter.permitirIntento(anyString())).thenReturn(true);
        when(usuarioRepository.findByEmail("noexiste@espe.edu.ec")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("noexiste@espe.edu.ec", "cualquier1")))
                .isInstanceOf(InvalidCredentialsException.class);

        // Se llama a matches() igual que si el usuario existiera (mismo costo de
        // tiempo), en vez de retornar antes por email no encontrado.
        verify(passwordEncoder).matches(eq("cualquier1"), anyString());
        verify(rateLimiter).registrarFallo("noexiste@espe.edu.ec");
    }

    @Test
    void login_passwordIncorrecta_lanzaInvalidCredentialsYRegistraFallo() {
        Usuario usuario = usuarioCon(UUID.randomUUID(), "ana@espe.edu.ec", "hash-real");
        when(rateLimiter.permitirIntento(anyString())).thenReturn(true);
        when(usuarioRepository.findByEmail("ana@espe.edu.ec")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta1", "hash-real")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@espe.edu.ec", "incorrecta1")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(rateLimiter).registrarFallo("ana@espe.edu.ec");
        verify(rateLimiter, never()).registrarExito(anyString());
    }

    @Test
    void login_credencialesValidas_devuelveTempTokenYSoloLosRolesDelUsuario() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioCon(usuarioId, "ana@espe.edu.ec", "hash-real");
        Rol admin = rolCon(UUID.randomUUID(), "ADMIN");
        UsuarioRol asignacion = new UsuarioRol(usuario, admin);

        when(rateLimiter.permitirIntento(anyString())).thenReturn(true);
        when(usuarioRepository.findByEmail("ana@espe.edu.ec")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("correcta1", "hash-real")).thenReturn(true);
        when(usuarioRolRepository.findByUsuarioId(usuarioId)).thenReturn(List.of(asignacion));

        LoginResponse response = authService.login(new LoginRequest("ana@espe.edu.ec", "correcta1"));

        Claims claims = jwtService.parseClaims(response.tempToken()).getPayload();
        assertThat(claims.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(claims.get(JwtService.CLAIM_TIPO, String.class)).isEqualTo(JwtService.TIPO_TEMP);
        assertThat(response.roles()).hasSize(1);
        assertThat(response.roles().get(0).nombre()).isEqualTo("ADMIN");
        verify(rateLimiter).registrarExito("ana@espe.edu.ec");
    }

    // ── selectRole ───────────────────────────────────────────────────────

    @Test
    void selectRole_tokenNoEsTemp_esRechazado() {
        UUID usuarioId = UUID.randomUUID();
        // Un access token (no un temp token) nunca debe servir para seleccionar rol.
        String accessToken = jwtService.generarAccessToken(usuarioId, UUID.randomUUID(), "ADMIN", List.of());

        assertThatThrownBy(() -> authService.selectRole(new SelectRoleRequest(accessToken, UUID.randomUUID())))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void selectRole_tokenMalformado_esRechazado() {
        assertThatThrownBy(() -> authService.selectRole(new SelectRoleRequest("esto-no-es-un-jwt", UUID.randomUUID())))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void selectRole_usuarioNoTieneElRolSolicitado_lanzaRoleNotAssignedException() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        String tempToken = jwtService.generarTempToken(usuarioId);
        when(usuarioRolRepository.findByUsuarioIdAndRolId(usuarioId, rolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.selectRole(new SelectRoleRequest(tempToken, rolId)))
                .isInstanceOf(RoleNotAssignedException.class);
    }

    @Test
    void selectRole_exitoso_elAccessTokenSoloLlevaLosPermisosDelRolElegido() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioCon(usuarioId, "ana@espe.edu.ec", "hash");
        Rol vendedor = rolCon(UUID.randomUUID(), "Vendedor");
        UsuarioRol asignacion = new UsuarioRol(usuario, vendedor);

        Menu itemVentas = new Menu();
        itemVentas.setUrl("/ventas/crear-orden");
        RolMenu permisoVentas = new RolMenu(vendedor, itemVentas);

        String tempToken = jwtService.generarTempToken(usuarioId);
        when(usuarioRolRepository.findByUsuarioIdAndRolId(usuarioId, vendedor.getId()))
                .thenReturn(Optional.of(asignacion));
        when(rolMenuRepository.findByRolId(vendedor.getId())).thenReturn(List.of(permisoVentas));

        SelectRoleResponse response = authService.selectRole(new SelectRoleRequest(tempToken, vendedor.getId()));

        Claims accessClaims = jwtService.parseClaims(response.accessToken()).getPayload();
        assertThat(accessClaims.get(JwtService.CLAIM_TIPO, String.class)).isEqualTo(JwtService.TIPO_ACCESS);
        assertThat(accessClaims.get(JwtService.CLAIM_ROL_ID, String.class)).isEqualTo(vendedor.getId().toString());
        assertThat(accessClaims.get(JwtService.CLAIM_PERMISOS, List.class)).containsExactly("/ventas/crear-orden");

        Claims refreshClaims = jwtService.parseClaims(response.refreshToken()).getPayload();
        assertThat(refreshClaims.get(JwtService.CLAIM_TIPO, String.class)).isEqualTo(JwtService.TIPO_REFRESH);

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // ── refrescar (rotación + detección de reuso) ───────────────────────

    @Test
    void refrescar_tokenNoEsRefresh_esRechazado() {
        String accessToken = jwtService.generarAccessToken(UUID.randomUUID(), UUID.randomUUID(), "ADMIN", List.of());

        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(accessToken)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refrescar_hashNoRegistrado_esRechazado() {
        String refreshToken = jwtService.generarRefreshToken(UUID.randomUUID(), UUID.randomUUID());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(refreshToken)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refrescar_tokenYaCanjeado_revocaTodaLaCadenaPorPosibleRobo() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        String refreshToken = jwtService.generarRefreshToken(usuarioId, rolId);

        RefreshToken registroReenviado = new RefreshToken();
        registroReenviado.setTokenHash(TokenHasher.sha256Hex(refreshToken));
        registroReenviado.inactivar(); // ya fue canjeado antes: esto es un reuso

        RefreshToken otraSesionActiva = new RefreshToken();
        otraSesionActiva.setFechaExpiracion(Instant.now().plusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(registroReenviado));
        when(refreshTokenRepository.findByUsuarioIdAndRolId(usuarioId, rolId))
                .thenReturn(List.of(otraSesionActiva));

        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(refreshToken)))
                .isInstanceOf(InvalidTokenException.class);

        assertThat(otraSesionActiva.estaActivo()).isFalse();
        verify(refreshTokenRepository).save(otraSesionActiva);
    }

    @Test
    void refrescar_tokenExpirado_seInactivaYSeRechaza() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        String refreshToken = jwtService.generarRefreshToken(usuarioId, rolId);

        RefreshToken registro = new RefreshToken();
        registro.setTokenHash(TokenHasher.sha256Hex(refreshToken));
        registro.setFechaExpiracion(Instant.now().minusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(registro));

        assertThatThrownBy(() -> authService.refrescar(new RefreshTokenRequest(refreshToken)))
                .isInstanceOf(InvalidTokenException.class);

        assertThat(registro.estaActivo()).isFalse();
        verify(refreshTokenRepository).save(registro);
    }

    @Test
    void refrescar_exitoso_rotaElTokenManteniendoElMismoRol() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioCon(usuarioId, "ana@espe.edu.ec", "hash");
        Rol vendedor = rolCon(UUID.randomUUID(), "Vendedor");
        String refreshToken = jwtService.generarRefreshToken(usuarioId, vendedor.getId());

        RefreshToken registro = new RefreshToken();
        registro.setTokenHash(TokenHasher.sha256Hex(refreshToken));
        registro.setFechaExpiracion(Instant.now().plusSeconds(60));
        registro.setUsuario(usuario);
        registro.setRol(vendedor);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(registro));
        when(rolMenuRepository.findByRolId(vendedor.getId())).thenReturn(List.of());

        RefreshTokenResponse response = authService.refrescar(new RefreshTokenRequest(refreshToken));

        assertThat(registro.estaActivo()).isFalse(); // el token viejo queda inutilizable
        Claims nuevoAccess = jwtService.parseClaims(response.accessToken()).getPayload();
        assertThat(nuevoAccess.get(JwtService.CLAIM_ROL_ID, String.class)).isEqualTo(vendedor.getId().toString());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    // ── logout ───────────────────────────────────────────────────────────

    @Test
    void logout_revocaSoloLosTokensActivosDeLaSesionDelPrincipal() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser(usuarioId, rolId, "ADMIN", List.of());

        RefreshToken activo = new RefreshToken();
        RefreshToken yaInactivo = new RefreshToken();
        yaInactivo.inactivar();

        when(refreshTokenRepository.findByUsuarioIdAndRolId(usuarioId, rolId))
                .thenReturn(List.of(activo, yaInactivo));

        authService.logout(principal);

        assertThat(activo.estaActivo()).isFalse();
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    // ── validarToken ─────────────────────────────────────────────────────

    @Test
    void validarToken_tipoIncorrecto_esRechazado() {
        String tempToken = jwtService.generarTempToken(UUID.randomUUID());

        assertThatThrownBy(() -> authService.validarToken(tempToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validarToken_accessTokenValido_devuelveClaimsDeLaSesion() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        String accessToken = jwtService.generarAccessToken(usuarioId, rolId, "Vendedor", List.of("/ventas"));

        var response = authService.validarToken(accessToken);

        assertThat(response.userId()).isEqualTo(usuarioId);
        assertThat(response.roleId()).isEqualTo(rolId);
        assertThat(response.role()).isEqualTo("Vendedor");
        assertThat(response.permissions()).containsExactly("/ventas");
    }
}
