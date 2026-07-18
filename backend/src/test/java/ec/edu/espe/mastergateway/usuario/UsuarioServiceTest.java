package ec.edu.espe.mastergateway.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.edu.espe.mastergateway.common.exception.DuplicateResourceException;
import ec.edu.espe.mastergateway.common.exception.ResourceNotFoundException;
import ec.edu.espe.mastergateway.usuario.dto.CreateUsuarioRequest;
import ec.edu.espe.mastergateway.usuario.dto.UpdateUsuarioRequest;
import ec.edu.espe.mastergateway.usuario.dto.UsuarioResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    @Test
    void crear_emailDuplicado_lanzaDuplicateResourceExceptionYNoHasheaNiPersiste() {
        when(usuarioRepository.existsByEmail("ana@espe.edu.ec")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(
                new CreateUsuarioRequest("ana@espe.edu.ec", "clave1234", "Ana")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_normalizaElEmailAMinusculasAntesDeValidarYGuardar() {
        when(usuarioRepository.existsByEmail("ana@espe.edu.ec")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse response = usuarioService.crear(
                new CreateUsuarioRequest("  Ana@ESPE.edu.ec ", "clave1234", "Ana"));

        assertThat(response.email()).isEqualTo("ana@espe.edu.ec");
    }

    @Test
    void crear_nuncaGuardaLaContrasenaEnTextoPlano() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("miClaveSecreta1")).thenReturn("$2a$12$hashBcryptSimulado");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.crear(new CreateUsuarioRequest("ana@espe.edu.ec", "miClaveSecreta1", "Ana"));

        Usuario guardado = captor.getValue();
        assertThat(guardado.getPasswordHash())
                .isEqualTo("$2a$12$hashBcryptSimulado")
                .isNotEqualTo("miClaveSecreta1");
    }

    @Test
    void obtener_usuarioInexistente_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtener(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_emailYaUsadoPorOtroUsuario_lanzaDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        Usuario existente = new Usuario();
        ReflectionTestUtils.setField(existente, "id", id);
        existente.setEmail("ana@espe.edu.ec");
        existente.setPasswordHash("hash");
        existente.setNombreCompleto("Ana");

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmailAndIdNot("otro@espe.edu.ec", id)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizar(id, new UpdateUsuarioRequest("otro@espe.edu.ec", "Ana")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void actualizar_noPermiteCambiarLaContrasenaPorEsteEndpoint() {
        // UpdateUsuarioRequest deliberadamente no tiene campo password: el cambio
        // de contraseña debe pasar por un flujo dedicado, no por un PUT genérico
        // que un atacante con el token de otro campo pudiera reutilizar.
        UUID id = UUID.randomUUID();
        Usuario existente = new Usuario();
        ReflectionTestUtils.setField(existente, "id", id);
        existente.setEmail("ana@espe.edu.ec");
        existente.setPasswordHash("hash-original");
        existente.setNombreCompleto("Ana");

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmailAndIdNot(eq("ana@espe.edu.ec"), eq(id))).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.actualizar(id, new UpdateUsuarioRequest("ana@espe.edu.ec", "Ana Maria"));

        assertThat(existente.getPasswordHash()).isEqualTo("hash-original");
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void eliminar_usuarioInexistente_lanzaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.eliminar(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(usuarioRepository, never()).delete(any());
    }
}
