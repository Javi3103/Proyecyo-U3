package ec.edu.espe.mastergateway.common.exception;

import org.springframework.http.HttpStatus;

public class RoleNotAssignedException extends ApiException {
    public RoleNotAssignedException() {
        super(HttpStatus.FORBIDDEN, "ROLE_NOT_ASSIGNED", "El usuario no posee el rol solicitado.");
    }
}
