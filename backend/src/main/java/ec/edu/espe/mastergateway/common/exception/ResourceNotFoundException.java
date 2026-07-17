package ec.edu.espe.mastergateway.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String recurso) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", recurso + " no encontrado.");
    }
}
