package com.alocode.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.alocode.service.CajaService;
import com.alocode.service.DashboardService;
import com.alocode.service.MyUserDetails;
import com.alocode.repository.ClienteConfiguracionRepository;

import lombok.RequiredArgsConstructor;

import com.alocode.model.Usuario;
import com.alocode.model.ClienteConfiguracion;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final CajaService cajaService;
    private final DashboardService dashboardService;
    private final ClienteConfiguracionRepository clienteConfiguracionRepository;

    @GetMapping({"/", "/home"})
    public String home(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        // Buscar la caja abierta de hoy (si existe)
        var cajaOpt = cajaService.obtenerCajaAbiertaHoy();
        model.addAttribute("cajaAbierta", cajaOpt.orElse(null));
        
        // Agregar usuario autenticado al modelo para el navbar
        Usuario usuario = null;
        if (userDetails instanceof MyUserDetails myUserDetails) {
            usuario = myUserDetails.getUsuario();
        }
        model.addAttribute("usuario", usuario);
        
        // Obtener el clienteId del contexto (tenant)
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        
        // Datos del dashboard
        Map<String, Object> ventasUltimos7Dias = dashboardService.obtenerVentasUltimos7Dias(clienteId);
        List<Map<String, Object>> top10Productos = dashboardService.obtenerTop10ProductosMasVendidos(clienteId);
        List<Map<String, Object>> productosStockBajo = dashboardService.obtenerProductosStockBajo(clienteId);
        
        model.addAttribute("ventasUltimos7Dias", ventasUltimos7Dias);
        model.addAttribute("top10Productos", top10Productos);
        model.addAttribute("productosStockBajo", productosStockBajo);
        
        // Obtener configuración del cliente para el formateo de stock
        if (clienteId != null) {
            ClienteConfiguracion config = clienteConfiguracionRepository.findByClienteId(clienteId);
            model.addAttribute("permitirProductosDerivados", 
                config != null ? config.getPermitirProductosDerivados() : true);
        }
        
        return "home";
    }
}
