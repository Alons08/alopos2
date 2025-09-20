package com.alocode.controller;

import com.alocode.model.Cliente;
import com.alocode.model.Usuario;
import com.alocode.service.ClienteService;
import com.alocode.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.alocode.service.MyUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@Controller
@RequestMapping("/cliente")
public class ClienteController {
    @Autowired
    private ClienteService clienteService;

    // Visualizar información principal del cliente (solo ADMIN)
    @GetMapping("")
    public String verCliente(@AuthenticationPrincipal MyUserDetails userDetails, Model model) {
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : null;
        if (usuario == null || usuario.getRoles().stream().noneMatch(r -> r.getNombre().equals("ADMIN"))) {
            return "redirect:/home";
        }
        Long clienteId = usuario.getCliente().getId();
        Optional<Cliente> optCliente = clienteService.findById(clienteId);
        if (optCliente.isEmpty()) {
            model.addAttribute("error", "No se encontró la información de la empresa");
            return "cliente-empresa"; // redirecciona a cliente-empresa.html
        }
        Cliente cliente = optCliente.get();
        model.addAttribute("cliente", cliente);
    return "cliente-empresa"; // redirecciona a cliente-empresa.html
    }

    // Mostrar formulario de edición
    @GetMapping("/editar")
    public String mostrarFormularioEdicion(@AuthenticationPrincipal MyUserDetails userDetails, Model model) {
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : null;
        if (usuario == null || usuario.getRoles().stream().noneMatch(r -> r.getNombre().equals("ADMIN"))) {
            return "redirect:/home";
        }
        Cliente cliente = usuario.getCliente();
        model.addAttribute("cliente", cliente);
    return "cliente-empresa"; // redirecciona a cliente-empresa.html
    }

    // Procesar edición
    @PostMapping("/editar")
    public String editarCliente(@ModelAttribute Cliente datosActualizados,
            @AuthenticationPrincipal MyUserDetails userDetails, Model model) {
        Usuario usuario = userDetails != null ? userDetails.getUsuario() : null;
        if (usuario == null || usuario.getRoles().stream().noneMatch(r -> r.getNombre().equals("ADMIN"))) {
            return "redirect:/home";
        }
        Long clienteId = usuario.getCliente().getId();
        boolean actualizado = clienteService.editarCliente(clienteId, datosActualizados, usuario);
        if (actualizado) {
            model.addAttribute("mensaje", "Datos actualizados correctamente");
        } else {
            model.addAttribute("error", "No tienes permisos para editar estos datos");
        }
        // Recargar el cliente actualizado desde el servicio
        Optional<Cliente> optCliente = clienteService.findById(clienteId);
        if (optCliente.isPresent()) {
            model.addAttribute("cliente", optCliente.get());
        } else {
            model.addAttribute("cliente", null);
        }
    return "cliente-empresa"; // redirecciona a cliente-empresa.html
    }
}
