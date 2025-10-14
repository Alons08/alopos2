package com.alocode.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alocode.model.Mesa;
import com.alocode.model.enums.EstadoMesa;
import com.alocode.service.MesaService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/mesas")
@RequiredArgsConstructor
public class MesaController {
    private final MesaService mesaService;

    @GetMapping
    public String listarMesas(@RequestParam(value = "q", required = false) String q, Model model) {
    model.addAttribute("mesas", mesaService.buscarMesas(q));
    model.addAttribute("totalActivas", mesaService.contarMesasActivas());
    model.addAttribute("totalInactivas", mesaService.contarMesasInactivas());
    return "mesas";
    }
    
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevaMesa(Model model) {
        model.addAttribute("mesa", new Mesa());
        model.addAttribute("estadosMesa", EstadoMesa.values());
        return "nueva-mesa";
    }
    
    @PostMapping("/guardar")
    public String guardarMesa(@ModelAttribute Mesa mesa, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Asignar el cliente actual usando TenantContext
            Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
            com.alocode.model.Cliente cliente = mesaService.obtenerClientePorId(clienteId);
            mesa.setCliente(cliente);
            mesaService.guardarMesa(mesa);
            redirectAttributes.addFlashAttribute("success", "Mesa guardada exitosamente");
            return "redirect:/mesas";
        } catch (IllegalArgumentException e) {
            model.addAttribute("mesa", mesa);
            model.addAttribute("estadosMesa", EstadoMesa.values());
            model.addAttribute("error", e.getMessage());
            return "nueva-mesa";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mesas/nuevo";
        }
    }
    
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarMesa(@PathVariable Long id, Model model) {
        model.addAttribute("mesa", mesaService.obtenerMesaPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada")));
        model.addAttribute("estadosMesa", EstadoMesa.values());
        return "nueva-mesa";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarMesa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            mesaService.eliminarMesa(id);
            redirectAttributes.addFlashAttribute("success", "Mesa eliminada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mesas";
    }

    @PostMapping("/desactivar/{id}")
    public String desactivarMesa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            mesaService.desactivarMesa(id);
            redirectAttributes.addFlashAttribute("success", "Mesa desactivada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mesas";
    }

    @PostMapping("/activar/{id}")
    public String activarMesa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            mesaService.activarMesa(id);
            redirectAttributes.addFlashAttribute("success", "Mesa activada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mesas";
    }
    
}