package com.alocode.controller;

import com.alocode.model.Usuario;
import com.alocode.service.UsuarioService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.findAll();
        model.addAttribute("usuarios", usuarios);
        return "usuarios";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
    model.addAttribute("usuario", new Usuario());
    model.addAttribute("roles", usuarioService.getRolesSinAdmin());
    model.addAttribute("esAdmin", false);
    model.addAttribute("usuarioForm", new Usuario());
    model.addAttribute("roles", usuarioService.getRolesSinAdmin());
    model.addAttribute("esAdmin", false);
    return "usuario-form";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, @RequestParam(required = false) List<Long> rolesIds, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
            com.alocode.model.Cliente cliente = usuarioService.obtenerClientePorId(clienteId);
            usuario.setCliente(cliente);
            usuarioService.guardarUsuario(usuario, rolesIds);
            redirectAttributes.addFlashAttribute("success", "Usuario guardado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.findById(id);
        model.addAttribute("usuarioForm", usuario);
        model.addAttribute("roles", usuarioService.getRolesSinAdmin());
        model.addAttribute("esAdmin", usuarioService.tieneRolAdmin(usuario));
        return "usuario-form";
    }

    @PostMapping("/actualizar")
    public String actualizarUsuario(@ModelAttribute Usuario usuario, @RequestParam(required = false) List<Long> rolesIds, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
            com.alocode.model.Cliente cliente = usuarioService.obtenerClientePorId(clienteId);
            usuario.setCliente(cliente);
            usuarioService.actualizarUsuario(usuario, rolesIds);
            redirectAttributes.addFlashAttribute("success", "Usuario actualizado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            usuarioService.cambiarEstadoeIntentosFallidos(id);
            redirectAttributes.addFlashAttribute("success", "Usuario desactivado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}
