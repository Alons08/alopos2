package com.alocode.security;

import com.alocode.model.Usuario;
import com.alocode.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

// Esta clase es un filtro que verifica si el usuario está activo en todas las solicitudes
@Component
public class UsuarioActivoFilter extends OncePerRequestFilter {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
            String username = authentication.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.getUserByUsername(username);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                
                // Verificar si el usuario está inactivo
                if (usuario.getActivo() == null || !usuario.getActivo()) {
                    // Usuario inactivo, invalidar sesión y redirigir
                    request.getSession().invalidate();
                    SecurityContextHolder.clearContext();
                    response.sendRedirect(request.getContextPath() + "/login?inactivo");
                    return;
                }
                
                // Si el usuario tiene cliente y no hay tenantId en sesión, establecerlo
                // (esto es importante para Remember Me)
                if (usuario.getCliente() != null) {
                    Object sessionTenant = request.getSession().getAttribute("tenantId");
                    if (sessionTenant == null) {
                        request.getSession().setAttribute("tenantId", usuario.getCliente().getId());
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
