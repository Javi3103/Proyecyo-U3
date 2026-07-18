package ec.edu.espe.mastergateway.rol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.edu.espe.mastergateway.acceso.RolMenuRepository;
import ec.edu.espe.mastergateway.acceso.RolModuloRepository;
import ec.edu.espe.mastergateway.acceso.UsuarioRol;
import ec.edu.espe.mastergateway.acceso.UsuarioRolRepository;
import ec.edu.espe.mastergateway.common.exception.DuplicateResourceException;
import ec.edu.espe.mastergateway.common.exception.ResourceNotFoundException;
import ec.edu.espe.mastergateway.common.exception.RoleInUseException;
import ec.edu.espe.mastergateway.menu.MenuRepository;
import ec.edu.espe.mastergateway.modulo.ModuloRepository;
import ec.edu.espe.mastergateway.rol.dto.CreateRolRequest;
import ec.edu.espe.mastergateway.rol.dto.RolResponse;
import ec.edu.espe.mastergateway.rol.dto.UpdateRolRequest;
import ec.edu.espe.mastergateway.usuario.Usuario;
import ec.edu.espe.mastergateway.usuario.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Foco en las reglas de negocio que protegen la integridad del modelo M:N
 * Usuario-Rol: nombres únicos, y el guard que evita borrar un rol mientras
 * siga en uso (para no dejar asignaciones huérfanas apuntando a un rol
 * inexistente).
 */
@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ModuloRepository moduloRepository;
    @Mock
    private MenuRepository menuRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private RolModuloRepository rolModuloRepository;
    @Mock
    private RolMenuRepository rolMenuRepository;

    private RolService rolService;

    @BeforeEach
    void setUp() {
        rolService = new RolService(
                rolRepository, usuarioRepository, moduloRepository, menuRepository,
                usuarioRolRepository, rolModuloRepository, rolMenuRepository);
    }

    private static Rol rolCon(UUID id, String nombre) {
        Rol rol = new Rol();
        ReflectionTestUtils.setField(rol, "id", id);
        rol.setNombre(nombre);
        return rol;
    }

    @Test
    void crear_nombreDuplicado_lanzaDuplicateResourceException() {
        when(rolRepository.existsByNombre("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> rolService.crear(new CreateRolRequest("ADMIN", "Administrador")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(rolRepository, never()).save(any());
    }

    @Test
    void crear_nombreLibre_persisteElRol() {
        when(rolRepository.existsByNombre("Vendedor")).thenReturn(false);
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> inv.getArgument(0));

        RolResponse response = rolService.crear(new CreateRolRequest("Vendedor", "Rol de ventas"));

        assertThat(response.nombre()).isEqualTo("Vendedor");
    }

    @Test
    void actualizar_nombreYaUsadoPorOtroRol_lanzaDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        Rol existente = rolCon(id, "Vendedor");
        when(rolRepository.findById(id)).thenReturn(Optional.of(existente));
        when(rolRepository.existsByNombreAndIdNot("ADMIN", id)).thenReturn(true);

        assertThatThrownBy(() -> rolService.actualizar(id, new UpdateRolRequest("ADMIN", "desc")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void eliminar_rolInexistente_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(rolRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolService.eliminar(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_rolAunAsignadoAUsuarios_lanzaRoleInUseException() {
        UUID id = UUID.randomUUID();
        Rol rol = rolCon(id, "ADMIN");
        when(rolRepository.findById(id)).thenReturn(Optional.of(rol));
        when(usuarioRolRepository.existsByRolId(id)).thenReturn(true);

        assertThatThrownBy(() -> rolService.eliminar(id))
                .isInstanceOf(RoleInUseException.class);

        verify(rolRepository, never()).delete(any());
    }

    @Test
    void eliminar_rolSinAsignaciones_seElimina() {
        UUID id = UUID.randomUUID();
        Rol rol = rolCon(id, "Temporal");
        when(rolRepository.findById(id)).thenReturn(Optional.of(rol));
        when(usuarioRolRepository.existsByRolId(id)).thenReturn(false);

        rolService.eliminar(id);

        verify(rolRepository).delete(rol);
    }

    @Test
    void asignarUsuario_usuarioInexistente_lanzaResourceNotFoundException() {
        UUID rolId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rolCon(rolId, "ADMIN")));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolService.asignarUsuario(rolId, usuarioId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void asignarUsuario_yaAsignado_lanzaDuplicateResourceException() {
        UUID rolId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rolCon(rolId, "ADMIN")));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(new Usuario()));
        when(usuarioRolRepository.existsByUsuarioIdAndRolId(usuarioId, rolId)).thenReturn(true);

        assertThatThrownBy(() -> rolService.asignarUsuario(rolId, usuarioId))
                .isInstanceOf(DuplicateResourceException.class);

        verify(usuarioRolRepository, never()).save(any(UsuarioRol.class));
    }

    @Test
    void asignarUsuario_libre_creaLaAsignacion() {
        UUID rolId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rolCon(rolId, "ADMIN")));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(new Usuario()));
        when(usuarioRolRepository.existsByUsuarioIdAndRolId(usuarioId, rolId)).thenReturn(false);

        rolService.asignarUsuario(rolId, usuarioId);

        verify(usuarioRolRepository).save(any(UsuarioRol.class));
    }
}
