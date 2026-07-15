package ec.edu.espe.mastergateway.common.exception;

import org.springframework.http.HttpStatus;

/** Bloquea la eliminación de un rol mientras siga asignado a usuarios activos. */
public class RoleInUseException extends ApiException {
    public RoleInUseException() {
        super(HttpStatus.CONFLICT, "ROLE_IN_USE", "El rol está asignado a usuarios activos y no puede eliminarse.");
    }
}
