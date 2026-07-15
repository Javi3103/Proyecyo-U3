package ec.edu.espe.mastergateway.menu;

import ec.edu.espe.mastergateway.common.exception.CyclicMenuReferenceException;
import ec.edu.espe.mastergateway.common.exception.ResourceNotFoundException;
import ec.edu.espe.mastergateway.menu.dto.CreateMenuRequest;
import ec.edu.espe.mastergateway.menu.dto.MenuNodoDto;
import ec.edu.espe.mastergateway.menu.dto.MenuResponse;
import ec.edu.espe.mastergateway.menu.dto.UpdateMenuRequest;
import ec.edu.espe.mastergateway.modulo.Modulo;
import ec.edu.espe.mastergateway.modulo.ModuloRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final ModuloRepository moduloRepository;

    /** GET /api/menus/tree: árbol jerárquico completo del rol activo en el token (Least Privilege). */
    @Transactional(readOnly = true)
    public List<MenuNodoDto> obtenerArbol(UUID rolId) {
        List<Menu> nodos = menuRepository.findArbolByRolId(rolId);

        Map<UUID, List<Menu>> hijosPorPadre = nodos.stream()
                .filter(menu -> menu.getParent() != null)
                .collect(Collectors.groupingBy(menu -> menu.getParent().getId()));

        return nodos.stream()
                .filter(menu -> menu.getParent() == null)
                .sorted(Comparator.comparing(Menu::getNombre))
                .map(raiz -> construirNodo(raiz, hijosPorPadre))
                .toList();
    }

    private MenuNodoDto construirNodo(Menu menu, Map<UUID, List<Menu>> hijosPorPadre) {
        List<MenuNodoDto> hijos = hijosPorPadre.getOrDefault(menu.getId(), List.of()).stream()
                .sorted(Comparator.comparing(Menu::getNombre))
                .map(hijo -> construirNodo(hijo, hijosPorPadre))
                .toList();
        return new MenuNodoDto(menu.getId(), menu.getNombre(), menu.getUrl(), menu.getModulo().getId(), hijos);
    }

    @Transactional
    public MenuResponse crear(CreateMenuRequest request) {
        Modulo modulo = moduloRepository.findById(request.moduloId())
                .orElseThrow(() -> new ResourceNotFoundException("Módulo"));

        Menu parent = null;
        if (request.parentId() != null) {
            parent = obtenerEntidad(request.parentId());
        }

        Menu menu = new Menu();
        menu.setNombre(request.nombre());
        menu.setUrl(request.url());
        menu.setModulo(modulo);
        menu.setParent(parent);
        return MenuResponse.from(menuRepository.save(menu));
    }

    @Transactional
    public MenuResponse actualizar(UUID id, UpdateMenuRequest request) {
        Menu menu = obtenerEntidad(id);

        Menu nuevoParent = null;
        if (request.parentId() != null) {
            validarSinCiclo(id, request.parentId());
            nuevoParent = obtenerEntidad(request.parentId());
        }

        menu.setNombre(request.nombre());
        menu.setUrl(request.url());
        menu.setParent(nuevoParent);
        return MenuResponse.from(menuRepository.save(menu));
    }

    @Transactional
    public void eliminar(UUID id) {
        Menu menu = obtenerEntidad(id);
        menuRepository.delete(menu);
    }

    /** Recorre la cadena de ancestros del nuevo padre para asegurar que el propio nodo no aparezca en ella. */
    private void validarSinCiclo(UUID menuId, UUID nuevoParentId) {
        UUID actual = nuevoParentId;
        while (actual != null) {
            if (actual.equals(menuId)) {
                throw new CyclicMenuReferenceException();
            }
            actual = menuRepository.findById(actual)
                    .map(Menu::getParent)
                    .map(Menu::getId)
                    .orElse(null);
        }
    }

    private Menu obtenerEntidad(UUID id) {
        return menuRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Menú"));
    }
}
