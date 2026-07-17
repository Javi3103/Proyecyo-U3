package ec.edu.espe.mastergateway.modulo;

import ec.edu.espe.mastergateway.modulo.dto.CreateModuloRequest;
import ec.edu.espe.mastergateway.modulo.dto.ModuloResponse;
import ec.edu.espe.mastergateway.modulo.dto.UpdateModuloRequest;
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
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuloController {

    private final ModuloService moduloService;

    @GetMapping
    public List<ModuloResponse> listar() {
        return moduloService.listar();
    }

    @GetMapping("/{id}")
    public ModuloResponse obtener(@PathVariable UUID id) {
        return moduloService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ModuloResponse> crear(@Valid @RequestBody CreateModuloRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moduloService.crear(request));
    }

    @PutMapping("/{id}")
    public ModuloResponse actualizar(@PathVariable UUID id, @Valid @RequestBody UpdateModuloRequest request) {
        return moduloService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        moduloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
