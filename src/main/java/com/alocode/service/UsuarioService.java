package com.alocode.service;

import com.alocode.model.Usuario;
import com.alocode.model.Rol;
import com.alocode.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import com.alocode.repository.RolRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // Listar todos los usuarios
    public List<Usuario> findAll() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return usuarioRepository.findAllByClienteIdOrderByIdAsc(clienteId);
    }

    // Buscar usuario por ID
    public Usuario findById(Long id) {
        Optional<Usuario> opt = usuarioRepository.findById(id);
        return opt.orElse(null);
    }

    // Obtener roles excepto ADMIN
    public List<Rol> getRolesSinAdmin() {
        return rolRepository.findAll().stream()
                .filter(rol -> !"ADMIN".equalsIgnoreCase(rol.getNombre()))
                .collect(Collectors.toList());
    }

    // Verifica si el usuario tiene rol ADMIN
    public boolean tieneRolAdmin(Usuario usuario) {
        return usuario.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getNombre()));
    }

    // Guardar nuevo usuario
    public void guardarUsuario(Usuario usuario, List<Long> rolesIds) {
        if (usuario.getId() == null && usuario.getPassword() != null) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        if (rolesIds != null) {
            Set<Rol> roles = new HashSet<>(rolRepository.findAllById(rolesIds));
            // Filtrar para que no se pueda asignar el rol ADMIN
            roles.removeIf(rol -> "ADMIN".equalsIgnoreCase(rol.getNombre()));
            usuario.setRoles(roles);
        }
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    // Actualizar usuario (sin permitir cambiar roles ADMIN)
    public void actualizarUsuario(Usuario usuario, List<Long> rolesIds) {
        Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(null);
        if (existente == null) return;
        existente.setNombre(usuario.getNombre());
        existente.setUsername(usuario.getUsername());
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        if (!tieneRolAdmin(existente) && rolesIds != null) {
            Set<Rol> roles = new java.util.HashSet<>(rolRepository.findAllById(rolesIds));
            // Filtrar para que no se pueda asignar el rol ADMIN
            roles.removeIf(rol -> "ADMIN".equalsIgnoreCase(rol.getNombre()));
            existente.setRoles(roles);
        }
        usuarioRepository.save(existente);
    }

    // Cambiar estado activo/inactivo
    public void cambiarEstadoeIntentosFallidos(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario != null && !tieneRolAdmin(usuario)) {
            usuario.setActivo(!usuario.getActivo());
            usuario.setIntentosFallidos(0); // Reinicia los intentos fallidos
            usuarioRepository.save(usuario);
        }
    }

    public boolean verificarPassword(Usuario usuario, String passwordActual) {
        return passwordEncoder.matches(passwordActual, usuario.getPassword());
    }

    @Transactional
    public void actualizarPassword(Usuario usuario, String nuevaPassword) {
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void actualizarNombre(Usuario usuario, String nombre) {
        usuario.setNombre(nombre);
        usuarioRepository.save(usuario);
    }
}