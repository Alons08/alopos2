package com.alocode.security;

import com.alocode.model.Usuario;
import com.alocode.repository.UsuarioRepository;
import com.alocode.service.MyUserDetails;
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
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof MyUserDetails) {
            MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            Optional<Usuario> usuarioOpt = usuarioRepository.getUserByUsername(username);
            if (usuarioOpt.isPresent() && (usuarioOpt.get().getActivo() == null || !usuarioOpt.get().getActivo())) {
                // Usuario inactivo, invalidar sesión y redirigir
                request.getSession().invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?inactivo");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
