package ec.edu.espe.mastergateway.common.exception;

import org.springframework.http.HttpStatus;

/** El nuevo parent_id de un menú generaría un bucle infinito en el árbol. */
public class CyclicMenuReferenceException extends ApiException {
    public CyclicMenuReferenceException() {
        super(HttpStatus.BAD_REQUEST, "CYCLIC_MENU_REFERENCE", "El padre indicado generaría una referencia cíclica.");
    }
}
