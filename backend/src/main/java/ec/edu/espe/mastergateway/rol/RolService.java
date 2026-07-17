package ec.edu.espe.mastergateway.rol;

import ec.edu.espe.mastergateway.acceso.RolMenu;
import ec.edu.espe.mastergateway.acceso.RolMenuRepository;
import ec.edu.espe.mastergateway.acceso.RolModulo;
import ec.edu.espe.mastergateway.acceso.RolModuloRepository;
import ec.edu.espe.mastergateway.acceso.UsuarioRol;
import ec.edu.espe.mastergateway.acceso.UsuarioRolRepository;
import ec.edu.espe.mastergateway.common.exception.DuplicateResourceException;
import ec.edu.espe.mastergateway.common.exception.ResourceNotFoundException;
import ec.edu.espe.mastergateway.common.exception.RoleInUseException;
import ec.edu.espe.mastergateway.menu.Menu;
import ec.edu.espe.mastergateway.menu.MenuRepository;
import ec.edu.espe.mastergateway.modulo.Modulo;
import ec.edu.espe.mastergateway.modulo.ModuloRepository;
import ec.edu.espe.mastergateway.rol.dto.CreateRolRequest;
import ec.edu.espe.mastergateway.rol.dto.RolResponse;
import ec.edu.espe.mastergateway.rol.dto.UpdateRolRequest;
import ec.edu.espe.mastergateway.usuario.Usuario;
import ec.edu.espe.mastergateway.usuario.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloRepository moduloRepository;
    private final MenuRepository menuRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolModuloRepository rolModuloRepository;
    private final RolMenuRepository rolMenuRepository;

    @Transactional(readOnly = true)
    public List<RolResponse> listar() {
        return rolRepository.findAll().stream().map(RolResponse::from).toList();
    }

    @Transactional
    public RolResponse crear(CreateRolRequest request) {
        if (rolRepository.existsByNombre(request.nombre())) {
            throw new DuplicateResourceException("Ya existe un rol con ese nombre.");
        }

        Rol rol = new Rol();
        rol.setNombre(request.nombre());
        rol.setDescripcion(request.descripcion());
        return RolResponse.from(rolRepository.save(rol));
    }

    @Transactional
    public RolResponse actualizar(UUID id, UpdateRolRequest request) {
        Rol rol = obtenerEntidad(id);
        if (rolRepository.existsByNombreAndIdNot(request.nombre(), id)) {
            throw new DuplicateResourceException("Ya existe un rol con ese nombre.");
        }

        rol.setNombre(request.nombre());
        rol.setDescripcion(request.descripcion());
        return RolResponse.from(rolRepository.save(rol));
    }

    @Transactional
    public void eliminar(UUID id) {
        Rol rol = obtenerEntidad(id);
        if (usuarioRolRepository.existsByRolId(id)) {
            throw new RoleInUseException();
        }
        rolRepository.delete(rol);
    }

    @Transactional
    public void asignarUsuario(UUID rolId, UUID usuarioId) {
        Rol rol = obtenerEntidad(rolId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario"));
        if (usuarioRolRepository.existsByUsuarioIdAndRolId(usuarioId, rolId)) {
            throw new DuplicateResourceException("El usuario ya tiene asignado este rol.");
        }
        usuarioRolRepository.save(new UsuarioRol(usuario, rol));
    }

    @Transactional
    public void desasignarUsuario(UUID rolId, UUID usuarioId) {
        if (!usuarioRolRepository.existsByUsuarioIdAndRolId(usuarioId, rolId)) {
            throw new ResourceNotFoundException("Asignación de rol");
        }
        usuarioRolRepository.hardDeleteByUsuarioIdAndRolId(usuarioId, rolId);
    }

    @Transactional
    public void asignarModulo(UUID rolId, UUID moduloId) {
        Rol rol = obtenerEntidad(rolId);
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo"));
        if (rolModuloRepository.existsByRolIdAndModuloId(rolId, moduloId)) {
            throw new DuplicateResourceException("El módulo ya está asignado a este rol.");
        }
        rolModuloRepository.save(new RolModulo(rol, modulo));
    }

    @Transactional
    public void asignarMenu(UUID rolId, UUID menuId) {
        Rol rol = obtenerEntidad(rolId);
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menú"));
        if (rolMenuRepository.existsByRolIdAndMenuId(rolId, menuId)) {
            throw new DuplicateResourceException("El menú ya está asignado a este rol.");
        }
        rolMenuRepository.save(new RolMenu(rol, menu));
    }

    private Rol obtenerEntidad(UUID id) {
        return rolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rol"));
    }
}
