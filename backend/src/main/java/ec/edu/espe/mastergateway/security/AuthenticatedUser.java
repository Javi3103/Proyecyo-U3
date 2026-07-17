package ec.edu.espe.mastergateway.security;

import java.util.List;
import java.util.UUID;

/**
 * Principal de seguridad reconstruido a partir de un Access Token válido.
 * rolId, rolNombre y permisos existen SOLO para el rol seleccionado en
 * /select-role (Least Privilege): nunca cargan roles o permisos que el
 * usuario tenga pero no eligió para esta sesión.
 */
public record AuthenticatedUser(UUID userId, UUID rolId, String rolNombre, List<String> permisos) {
}
