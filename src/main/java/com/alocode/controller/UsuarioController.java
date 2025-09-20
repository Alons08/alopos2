package com.alocode.controller;

import com.alocode.model.Usuario;
import com.alocode.service.UsuarioService;
import com.alocode.service.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping
    public String mostrarPerfil(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Usuario usuario = null;
        if (userDetails instanceof MyUserDetails myUserDetails) {
            usuario = myUserDetails.getUsuario();
        }
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @PostMapping("/actualizar")
    public String actualizarPerfil(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
                                   @RequestParam String nombre,
                                   @RequestParam String passwordActual,
                                   @RequestParam(required = false) String nuevaPassword,
                                   @RequestParam(required = false) String confirmarPassword,
                                   RedirectAttributes redirectAttributes) {
        Usuario usuario = null;
        if (userDetails instanceof MyUserDetails myUserDetails) {
            usuario = myUserDetails.getUsuario();
        }
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "No se pudo obtener el usuario actual");
            return "redirect:/perfil";
        }
        // Validar contraseña actual
        if (!usuarioService.verificarPassword(usuario, passwordActual)) {
            redirectAttributes.addFlashAttribute("error", "La contraseña actual es incorrecta");
            return "redirect:/perfil";
        }
        // Validar nueva contraseña si se quiere cambiar
        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            if (!nuevaPassword.equals(confirmarPassword)) {
                redirectAttributes.addFlashAttribute("error", "Las nuevas contraseñas no coinciden");
                return "redirect:/perfil";
            }
            usuarioService.actualizarPassword(usuario, nuevaPassword);
        }
        usuarioService.actualizarNombre(usuario, nombre);
        redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
        return "redirect:/perfil";
    }
}
