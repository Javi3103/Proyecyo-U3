package ec.edu.espe.mastergateway.menu;

import ec.edu.espe.mastergateway.menu.dto.CreateMenuRequest;
import ec.edu.espe.mastergateway.menu.dto.MenuNodoDto;
import ec.edu.espe.mastergateway.menu.dto.MenuResponse;
import ec.edu.espe.mastergateway.menu.dto.UpdateMenuRequest;
import ec.edu.espe.mastergateway.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public List<MenuNodoDto> obtenerArbol(@AuthenticationPrincipal AuthenticatedUser principal) {
        return menuService.obtenerArbol(principal.rolId());
    }

    @PostMapping
    public ResponseEntity<MenuResponse> crear(@Valid @RequestBody CreateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.crear(request));
    }

    @PutMapping("/{id}")
    public MenuResponse actualizar(@PathVariable UUID id, @Valid @RequestBody UpdateMenuRequest request) {
        return menuService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        menuService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
