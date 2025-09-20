package com.alocode.util;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@WebFilter("/*")
public class TenantFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
    Long tenantId = obtenerTenantIdDeRequest(httpRequest);
        if (tenantId != null) {
            TenantContext.setCurrentTenant(tenantId);
            httpRequest.getSession().setAttribute("tenantId", tenantId);
        } else {
            TenantContext.setCurrentTenant(null);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            //
            TenantContext.clear();
        }
    }
    
    private Long obtenerTenantIdDeRequest(HttpServletRequest request) {
        // 1. Intentar obtener de header HTTP
        String headerTenant = request.getHeader("X-Tenant-ID");
        if (headerTenant != null) {
            try {
                return Long.parseLong(headerTenant);
            } catch (NumberFormatException ignored) {}
        }

        // 2. Intentar obtener de parámetro en la URL
        String paramTenant = request.getParameter("tenantId");
        if (paramTenant != null) {
            try {
                return Long.parseLong(paramTenant);
            } catch (NumberFormatException ignored) {}
        }

        // 3. Intentar obtener de sesión (si el usuario ya está autenticado)
        Object sessionTenant = request.getSession().getAttribute("tenantId");
        if (sessionTenant instanceof Long) {
            return (Long) sessionTenant;
        } else if (sessionTenant instanceof String) {
            try {
                return Long.parseLong((String) sessionTenant);
            } catch (NumberFormatException ignored) {}
        }

        // 4. Intentar obtener del subdominio (ejemplo: cliente1.tuapp.com)
        String serverName = request.getServerName();
        // Implementa tu lógica para extraer el tenantId del subdominio si lo usas
        // Por ejemplo, podrías tener un mapa subdominio->tenantId

        // Si no se encuentra, retorna null o un valor por defecto
        return null;
    }
}