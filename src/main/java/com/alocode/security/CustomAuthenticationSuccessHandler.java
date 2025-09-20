package com.alocode.security;

import com.alocode.model.Usuario;
import com.alocode.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        Long tenantId = null;
        Object sessionTenant = request.getSession().getAttribute("tenantId");
        if (sessionTenant instanceof Long) {
            tenantId = (Long) sessionTenant;
        } else if (sessionTenant instanceof String) {
            try {
                tenantId = Long.parseLong((String) sessionTenant);
            } catch (NumberFormatException ignored) {}
        }
        Usuario usuario = null;
        if (tenantId != null) {
            usuario = usuarioRepository.findByClienteIdAndUsername(tenantId, username).orElse(null);
        } else {
            usuario = usuarioRepository.findByUsername(username);
        }
        if (usuario != null && usuario.getCliente() != null) {
            request.getSession().setAttribute("tenantId", usuario.getCliente().getId());
        } else {
        }
        response.sendRedirect("/home");
    }
}
