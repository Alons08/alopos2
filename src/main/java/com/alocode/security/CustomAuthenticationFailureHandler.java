package com.alocode.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Manejador personalizado para fallos de autenticación en login.
// Permite personalizar la respuesta al usuario cuando falla el login (redirigir, mostrar mensajes, etc).
// Se configura en WebSecurityConfig con .failureHandler(...)
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        response.sendRedirect("/login?error=true&username=" + (username != null ? username : ""));
    }
    
}
