package ec.edu.espe.mastergateway.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {
    public InvalidTokenException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token inválido o expirado.");
    }
}
