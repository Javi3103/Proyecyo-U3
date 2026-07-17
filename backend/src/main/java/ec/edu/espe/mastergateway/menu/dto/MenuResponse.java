package ec.edu.espe.mastergateway.menu.dto;

import ec.edu.espe.mastergateway.menu.Menu;
import java.util.UUID;

public record MenuResponse(UUID id, String nombre, String url, UUID moduloId, UUID parentId, String estado) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getNombre(),
                menu.getUrl(),
                menu.getModulo().getId(),
                menu.getParent() == null ? null : menu.getParent().getId(),
                menu.getEstado().name());
    }
}
