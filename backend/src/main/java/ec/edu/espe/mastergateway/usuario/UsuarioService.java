package ec.edu.espe.mastergateway.usuario;

import ec.edu.espe.mastergateway.common.exception.DuplicateResourceException;
import ec.edu.espe.mastergateway.common.exception.ResourceNotFoundException;
import ec.edu.espe.mastergateway.usuario.dto.CreateUsuarioRequest;
import ec.edu.espe.mastergateway.usuario.dto.UpdateUsuarioRequest;
import ec.edu.espe.mastergateway.usuario.dto.UsuarioResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponse::from);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(UUID id) {
        return UsuarioResponse.from(obtenerEntidad(id));
    }

    @Transactional
    public UsuarioResponse crear(CreateUsuarioRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Ya existe un usuario con ese email.");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(UUID id, UpdateUsuarioRequest request) {
        Usuario usuario = obtenerEntidad(id);
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("Ya existe un usuario con ese email.");
        }

        usuario.setEmail(email);
        usuario.setNombreCompleto(request.nombreCompleto());
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(UUID id) {
        Usuario usuario = obtenerEntidad(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario obtenerEntidad(UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario"));
    }
}
