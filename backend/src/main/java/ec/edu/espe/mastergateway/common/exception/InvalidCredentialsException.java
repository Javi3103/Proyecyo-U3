package ec.edu.espe.mastergateway.common.exception;

import org.springframework.http.HttpStatus;

/** Mensaje deliberadamente genérico: no revela si falló el email o la contraseña. */
public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Credenciales inválidas.");
    }
}
