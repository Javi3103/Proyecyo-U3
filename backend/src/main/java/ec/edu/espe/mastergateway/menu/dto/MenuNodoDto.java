package ec.edu.espe.mastergateway.menu.dto;

import java.util.List;
import java.util.UUID;

/** Nodo del árbol de menú ya anidado (Módulo -> Submenú -> Item), listo para que el frontend construya rutas. */
public record MenuNodoDto(UUID id, String nombre, String url, UUID moduloId, List<MenuNodoDto> hijos) {
}
