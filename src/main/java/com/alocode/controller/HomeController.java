package com.alocode.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.alocode.service.CajaService;
import com.alocode.service.MyUserDetails;

import lombok.RequiredArgsConstructor;

import com.alocode.model.Usuario;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final CajaService cajaService;

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
        return "home";
    }
}
