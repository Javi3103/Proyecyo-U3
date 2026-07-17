package ec.edu.espe.mastergateway.modulo;

import ec.edu.espe.mastergateway.common.exception.DuplicateResourceException;
import ec.edu.espe.mastergateway.common.exception.ResourceNotFoundException;
import ec.edu.espe.mastergateway.modulo.dto.CreateModuloRequest;
import ec.edu.espe.mastergateway.modulo.dto.ModuloResponse;
import ec.edu.espe.mastergateway.modulo.dto.UpdateModuloRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModuloService {

    private final ModuloRepository moduloRepository;

    @Transactional(readOnly = true)
    public List<ModuloResponse> listar() {
        return moduloRepository.findAll().stream().map(ModuloResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ModuloResponse obtener(UUID id) {
        return ModuloResponse.from(obtenerEntidad(id));
    }

    @Transactional
    public ModuloResponse crear(CreateModuloRequest request) {
        if (moduloRepository.existsByNombre(request.nombre())) {
            throw new DuplicateResourceException("Ya existe un módulo con ese nombre.");
        }

        Modulo modulo = new Modulo();
        modulo.setNombre(request.nombre());
        modulo.setDescripcion(request.descripcion());
        return ModuloResponse.from(moduloRepository.save(modulo));
    }

    @Transactional
    public ModuloResponse actualizar(UUID id, UpdateModuloRequest request) {
        Modulo modulo = obtenerEntidad(id);
        if (moduloRepository.existsByNombreAndIdNot(request.nombre(), id)) {
            throw new DuplicateResourceException("Ya existe un módulo con ese nombre.");
        }

        modulo.setNombre(request.nombre());
        modulo.setDescripcion(request.descripcion());
        return ModuloResponse.from(moduloRepository.save(modulo));
    }

    /** Inactivar un módulo (soft delete) hace que sus menús dejen de renderizarse: ver MenuRepository#findArbolByRolId. */
    @Transactional
    public void eliminar(UUID id) {
        Modulo modulo = obtenerEntidad(id);
        moduloRepository.delete(modulo);
    }

    private Modulo obtenerEntidad(UUID id) {
        return moduloRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Módulo"));
    }
}
