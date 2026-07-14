package ec.edu.espe.mastergateway.auth;

import ec.edu.espe.mastergateway.acceso.RolMenu;
import ec.edu.espe.mastergateway.acceso.RolMenuRepository;
import ec.edu.espe.mastergateway.acceso.UsuarioRol;
import ec.edu.espe.mastergateway.acceso.UsuarioRolRepository;
import ec.edu.espe.mastergateway.auth.dto.LoginRequest;
import ec.edu.espe.mastergateway.auth.dto.LoginResponse;
import ec.edu.espe.mastergateway.auth.dto.RefreshTokenRequest;
import ec.edu.espe.mastergateway.auth.dto.RefreshTokenResponse;
import ec.edu.espe.mastergateway.auth.dto.RolResumenDto;
import ec.edu.espe.mastergateway.auth.dto.SelectRoleRequest;
import ec.edu.espe.mastergateway.auth.dto.SelectRoleResponse;
import ec.edu.espe.mastergateway.auth.dto.ValidateTokenResponse;
import ec.edu.espe.mastergateway.common.exception.InvalidCredentialsException;
import ec.edu.espe.mastergateway.common.exception.InvalidTokenException;
import ec.edu.espe.mastergateway.common.exception.RoleNotAssignedException;
import ec.edu.espe.mastergateway.common.exception.RateLimitExceededException;
import ec.edu.espe.mastergateway.menu.Menu;
import ec.edu.espe.mastergateway.rol.Rol;
import ec.edu.espe.mastergateway.security.AuthenticatedUser;
import ec.edu.espe.mastergateway.security.JwtService;
import ec.edu.espe.mastergateway.security.TokenHasher;
import ec.edu.espe.mastergateway.usuario.Usuario;
import ec.edu.espe.mastergateway.usuario.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Hash Bcrypt válido pero de una contraseña que nadie tiene, usado cuando
     * el email no existe: así el tiempo de respuesta de un email inexistente
     * es equivalente al de una contraseña incorrecta (no revela cuál falló).
     */
    private static final String DUMMY_HASH =
            "$2a$12$sfB7e.JX8FjiFjB3nJgAhu4hJCAd/Q1I0Bbv63uS5w1LD7DV69d36";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolMenuRepository rolMenuRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String clave = request.email().trim().toLowerCase();
        if (!rateLimiter.permitirIntento(clave)) {
            throw new RateLimitExceededException();
        }

        var usuarioOpt = usuarioRepository.findByEmail(clave);
        String hashAValidar = usuarioOpt.map(Usuario::getPasswordHash).orElse(DUMMY_HASH);
        boolean credencialesValidas = passwordEncoder.matches(request.password(), hashAValidar) && usuarioOpt.isPresent();

        if (!credencialesValidas) {
            rateLimiter.registrarFallo(clave);
            throw new InvalidCredentialsException();
        }
        rateLimiter.registrarExito(clave);

        Usuario usuario = usuarioOpt.get();
        List<RolResumenDto> roles = usuarioRolRepository.findByUsuarioId(usuario.getId()).stream()
                .map(UsuarioRol::getRol)
                .map(rol -> new RolResumenDto(rol.getId(), rol.getNombre()))
                .toList();

        String tempToken = jwtService.generarTempToken(usuario.getId());
        return new LoginResponse(tempToken, roles);
    }

    @Transactional
    public SelectRoleResponse selectRole(SelectRoleRequest request) {
        Claims claims = validarTipo(request.tempToken(), JwtService.TIPO_TEMP);
        UUID usuarioId = UUID.fromString(claims.getSubject());

        UsuarioRol asignacion = usuarioRolRepository.findByUsuarioIdAndRolId(usuarioId, request.roleId())
                .orElseThrow(RoleNotAssignedException::new);

        Rol rol = asignacion.getRol();
        Usuario usuario = asignacion.getUsuario();
        List<String> permisos = permisosDelRol(rol.getId());

        String accessToken = jwtService.generarAccessToken(usuarioId, rol.getId(), rol.getNombre(), permisos);
        String refreshToken = jwtService.generarRefreshToken(usuarioId, rol.getId());
        guardarRefreshToken(refreshToken, usuario, rol);

        return new SelectRoleResponse(accessToken, refreshToken);
    }

    /**
     * noRollbackFor es intencional: cuando detectamos reutilización o expiración
     * lanzamos InvalidTokenException para que el cliente vea el fallo, pero la
     * revocación que acabamos de hacer (inactivar el token, revocar la cadena)
     * debe quedar committeada igual — si Spring hiciera rollback por defecto
     * (su comportamiento normal ante una RuntimeException), la revocación de
     * seguridad desaparecería junto con la transacción.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public RefreshTokenResponse refrescar(RefreshTokenRequest request) {
        Claims claims = validarTipo(request.refreshToken(), JwtService.TIPO_REFRESH);
        UUID usuarioId = UUID.fromString(claims.getSubject());
        UUID rolId = UUID.fromString(claims.get(JwtService.CLAIM_ROL_ID, String.class));

        String hash = TokenHasher.sha256Hex(request.refreshToken());
        RefreshToken registro = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidTokenException::new);

        if (!registro.estaActivo()) {
            // El token ya fue canjeado antes: esto es una reutilización, posible robo.
            // Se revoca toda la cadena de la sesión (usuario+rol) de inmediato.
            revocarTodos(usuarioId, rolId);
            throw new InvalidTokenException();
        }
        if (registro.getFechaExpiracion().isBefore(Instant.now())) {
            registro.inactivar();
            refreshTokenRepository.save(registro);
            throw new InvalidTokenException();
        }

        registro.inactivar();
        refreshTokenRepository.save(registro);

        Rol rol = registro.getRol();
        Usuario usuario = registro.getUsuario();
        List<String> permisos = permisosDelRol(rolId);

        String nuevoAccessToken = jwtService.generarAccessToken(usuarioId, rolId, rol.getNombre(), permisos);
        String nuevoRefreshToken = jwtService.generarRefreshToken(usuarioId, rolId);
        guardarRefreshToken(nuevoRefreshToken, usuario, rol);

        return new RefreshTokenResponse(nuevoAccessToken, nuevoRefreshToken);
    }

    @Transactional
    public void logout(AuthenticatedUser principal) {
        revocarTodos(principal.userId(), principal.rolId());
    }

    @Transactional(readOnly = true)
    public ValidateTokenResponse validarToken(String token) {
        Claims claims = validarTipo(token, JwtService.TIPO_ACCESS);
        UUID usuarioId = UUID.fromString(claims.getSubject());
        UUID rolId = UUID.fromString(claims.get(JwtService.CLAIM_ROL_ID, String.class));
        String rolNombre = claims.get(JwtService.CLAIM_ROL_NOMBRE, String.class);
        @SuppressWarnings("unchecked")
        List<String> permisos = claims.get(JwtService.CLAIM_PERMISOS, List.class);
        return new ValidateTokenResponse(usuarioId, rolId, rolNombre, permisos == null ? List.of() : permisos);
    }

    private List<String> permisosDelRol(UUID rolId) {
        return rolMenuRepository.findByRolId(rolId).stream()
                .map(RolMenu::getMenu)
                .map(Menu::getUrl)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Claims validarTipo(String token, String tipoEsperado) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException();
        }
        if (!tipoEsperado.equals(claims.get(JwtService.CLAIM_TIPO, String.class))) {
            throw new InvalidTokenException();
        }
        return claims;
    }

    private void revocarTodos(UUID usuarioId, UUID rolId) {
        refreshTokenRepository.findByUsuarioIdAndRolId(usuarioId, rolId).forEach(registro -> {
            if (registro.estaActivo()) {
                registro.inactivar();
                refreshTokenRepository.save(registro);
            }
        });
    }

    private void guardarRefreshToken(String rawToken, Usuario usuario, Rol rol) {
        RefreshToken registro = new RefreshToken();
        registro.setUsuario(usuario);
        registro.setRol(rol);
        registro.setTokenHash(TokenHasher.sha256Hex(rawToken));
        registro.setFechaExpiracion(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()));
        refreshTokenRepository.save(registro);
    }
}
