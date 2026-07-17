package ec.edu.espe.mastergateway.rol;

import ec.edu.espe.mastergateway.rol.dto.AsignarMenuRequest;
import ec.edu.espe.mastergateway.rol.dto.AsignarModuloRequest;
import ec.edu.espe.mastergateway.rol.dto.AsignarUsuarioRequest;
import ec.edu.espe.mastergateway.rol.dto.CreateRolRequest;
import ec.edu.espe.mastergateway.rol.dto.RolResponse;
import ec.edu.espe.mastergateway.rol.dto.UpdateRolRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    public List<RolResponse> listar() {
        return rolService.listar();
    }

    @PostMapping
    public ResponseEntity<RolResponse> crear(@Valid @RequestBody CreateRolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(request));
    }

    @PutMapping("/{id}")
    public RolResponse actualizar(@PathVariable UUID id, @Valid @RequestBody UpdateRolRequest request) {
        return rolService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        rolService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/users")
    public ResponseEntity<Void> asignarUsuario(@PathVariable UUID id, @Valid @RequestBody AsignarUsuarioRequest request) {
        rolService.asignarUsuario(id, request.usuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<Void> desasignarUsuario(@PathVariable UUID id, @PathVariable UUID userId) {
        rolService.desasignarUsuario(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/modules")
    public ResponseEntity<Void> asignarModulo(@PathVariable UUID id, @Valid @RequestBody AsignarModuloRequest request) {
        rolService.asignarModulo(id, request.moduloId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{id}/menus")
    public ResponseEntity<Void> asignarMenu(@PathVariable UUID id, @Valid @RequestBody AsignarMenuRequest request) {
        rolService.asignarMenu(id, request.menuId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
