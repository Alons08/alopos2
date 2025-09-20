package com.alocode.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alocode.model.Caja;
import com.alocode.model.Pedido;
import com.alocode.model.Usuario;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.service.CajaService;
import com.alocode.service.PedidoService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/caja")
@RequiredArgsConstructor
public class CajaController {
    private final CajaService cajaService;
    private final PedidoService pedidoService;

    @GetMapping("/abrir")
    public String mostrarFormularioAbrirCaja(Model model) {
        boolean cajaAbierta = cajaService.obtenerCajaAbiertaHoy().isPresent();
        model.addAttribute("cajaAbierta", cajaAbierta);
        model.addAttribute("montoApertura", 0.0);
        return "abrir-caja";
    }

    @PostMapping("/abrir")
    public String abrirCaja(@RequestParam Double montoApertura,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            com.alocode.model.Usuario usuario = null;
            if (userDetails instanceof com.alocode.service.MyUserDetails myUserDetails) {
                usuario = myUserDetails.getUsuario();
            }
            if (usuario == null) {
                throw new IllegalStateException("No se pudo obtener el usuario autenticado");
            }
            // Cancelar pedidos pendientes de días anteriores antes de abrir la caja
            pedidoService.cancelarPedidosPendientesDeDiasAnteriores();
            cajaService.abrirCaja(montoApertura, usuario);
            redirectAttributes.addFlashAttribute("success", "Caja abierta exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja/abrir";
        }
        return "redirect:/home";
    }

    @GetMapping("/cerrar")
    public String mostrarFormularioCerrarCaja(Model model, @AuthenticationPrincipal Usuario usuario) {
        boolean cajaAbierta = cajaService.obtenerCajaAbiertaHoy().isPresent();
        model.addAttribute("cajaAbierta", cajaAbierta);
        if (!cajaAbierta) {
            model.addAttribute("mensajeSinCaja", "No hay una caja abierta para hoy. No es posible cerrar caja.");
            return "cerrar-caja";
        }
        Optional<Caja> cajaOpt = cajaService.obtenerCajaAbiertaHoy();
        Caja caja = cajaOpt.get();
        // Obtener solo los pedidos PAGADOS de la caja actual
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorCajaYEstado(caja.getId(), EstadoPedido.PAGADO);
        double totalVentas = pedidos.stream()
                .mapToDouble(p -> p.getTotal() - p.getRecargo())
                .sum();
        double totalRecargos = pedidos.stream()
                .mapToDouble(Pedido::getRecargo)
                .sum();
        double totalNeto = totalVentas + totalRecargos;
        double montoCierre = caja.getMontoApertura() + totalNeto;
        model.addAttribute("caja", caja);
        model.addAttribute("totalVentas", totalVentas);
        model.addAttribute("totalRecargos", totalRecargos);
        model.addAttribute("totalNeto", totalNeto);
        model.addAttribute("montoCierre", montoCierre);
        return "cerrar-caja";
    }

    @PostMapping("/cerrar")
    public String cerrarCaja(@AuthenticationPrincipal Usuario usuario,
            RedirectAttributes redirectAttributes) {
        try {
            Caja caja = cajaService.obtenerCajaAbiertaHoy()
                    .orElseThrow(() -> new IllegalStateException("No hay caja abierta hoy"));

            cajaService.cerrarCaja(caja.getId(), usuario);
            redirectAttributes.addFlashAttribute("success", "Caja cerrada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/caja/cerrar";
        }
        return "redirect:/home";
    }
}