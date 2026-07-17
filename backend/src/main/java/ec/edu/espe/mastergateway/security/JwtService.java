package ec.edu.espe.mastergateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Emite y valida los 3 tipos de token del flujo de autenticación:
 * - TEMP: emitido por /login, solo sirve para llamar a /select-role.
 * - ACCESS: emitido por /select-role, acotado a un único rol (Least Privilege),
 *   expiración corta (Zero Trust). Lleva el nombre del rol embebido para que
 *   los microservicios hijos no tengan que consultar la base de datos en
 *   cada /validate-token.
 * - REFRESH: emitido junto al ACCESS, sirve únicamente para /refresh-token.
 *
 * El "tipo" viaja como claim y cada endpoint exige el tipo que le corresponde,
 * para que un TempToken nunca pueda usarse como credencial de acceso general.
 * Todos los tokens llevan un "jti" único (base para un futuro blocklist de
 * revocación inmediata de access tokens).
 */
@Component
public class JwtService {

    public static final String CLAIM_TIPO = "tipo";
    public static final String CLAIM_ROL_ID = "rolId";
    public static final String CLAIM_ROL_NOMBRE = "rolNombre";
    public static final String CLAIM_PERMISOS = "permisos";

    public static final String TIPO_TEMP = "TEMP";
    public static final String TIPO_ACCESS = "ACCESS";
    public static final String TIPO_REFRESH = "REFRESH";

    private final SecretKey signingKey;
    private final long tempTokenExpirationMs;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.temp-token-expiration-ms}") long tempTokenExpirationMs,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.tempTokenExpirationMs = tempTokenExpirationMs;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public String generarTempToken(UUID usuarioId) {
        return construir(usuarioId, tempTokenExpirationMs, Map.of(CLAIM_TIPO, TIPO_TEMP));
    }

    public String generarAccessToken(UUID usuarioId, UUID rolId, String rolNombre, List<String> permisos) {
        return construir(usuarioId, accessTokenExpirationMs, Map.of(
                CLAIM_TIPO, TIPO_ACCESS,
                CLAIM_ROL_ID, rolId.toString(),
                CLAIM_ROL_NOMBRE, rolNombre,
                CLAIM_PERMISOS, permisos));
    }

    public String generarRefreshToken(UUID usuarioId, UUID rolId) {
        return construir(usuarioId, refreshTokenExpirationMs, Map.of(
                CLAIM_TIPO, TIPO_REFRESH,
                CLAIM_ROL_ID, rolId.toString()));
    }

    /** Lanza JwtException (firma inválida, token expirado, malformado, etc.) si no es válido. */
    public Jws<Claims> parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
    }

    private String construir(UUID subject, long expirationMs, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject.toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
