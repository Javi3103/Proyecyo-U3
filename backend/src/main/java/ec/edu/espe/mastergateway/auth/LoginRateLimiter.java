package ec.edu.espe.mastergateway.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Rate limiting por email normalizado (ventana fija de 15 min, 5 intentos).
 * Implementación en memoria: suficiente para una única instancia (Railway/
 * Render free tier). Si el despliegue crece a múltiples instancias, esto
 * debe migrar a un contador compartido (ej. Redis) para no perder el conteo
 * entre instancias.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final Map<String, Intentos> intentosPorClave = new ConcurrentHashMap<>();

    public boolean permitirIntento(String clave) {
        Intentos intentos = intentosPorClave.get(clave);
        if (intentos == null || Instant.now().isAfter(intentos.ventanaExpiraEn())) {
            return true;
        }
        return intentos.contador().get() < MAX_INTENTOS;
    }

    public void registrarFallo(String clave) {
        intentosPorClave.compute(clave, (key, actual) -> {
            if (actual == null || Instant.now().isAfter(actual.ventanaExpiraEn())) {
                return new Intentos(new AtomicInteger(1), Instant.now().plus(VENTANA));
            }
            actual.contador().incrementAndGet();
            return actual;
        });
    }

    public void registrarExito(String clave) {
        intentosPorClave.remove(clave);
    }

    private record Intentos(AtomicInteger contador, Instant ventanaExpiraEn) {
    }
}
