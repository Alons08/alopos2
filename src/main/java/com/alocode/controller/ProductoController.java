package com.alocode.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alocode.model.Cliente;
import com.alocode.model.Producto;
import com.alocode.service.ProductoService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productoService;

    @GetMapping
    public String listarProductos(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "4") int size, // 50
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "dir", defaultValue = "asc") String dir,
            Model model) {
        Sort.Direction direction = dir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        Page<Producto> productosPage;
        if (q != null && !q.trim().isEmpty()) {
            productosPage = productoService.buscarProductosPorNombrePaginado(q, pageable);
        } else {
            productosPage = productoService.obtenerProductosPaginados(pageable);
        }
        model.addAttribute("productosPage", productosPage);
        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("productosBase", productoService.obtenerProductosBase());
        // Totales globales de productos activos/inactivos
        model.addAttribute("totalActivos", productoService.contarProductosActivos());
        model.addAttribute("totalInactivos", productoService.contarProductosInactivos());
        // Agregar el cliente actual al modelo para control de visibilidad
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Cliente cliente = productoService.obtenerClientePorId(clienteId);
        model.addAttribute("cliente", cliente);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "productos";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoProducto(Model model, @RequestParam(value = "id", required = false) Long id) {
        if (id != null) {
            model.addAttribute("producto", productoService.obtenerProductoPorId(id).orElse(new Producto()));
        } else {
            model.addAttribute("producto", new Producto());
        }
        // Agregar lista de productos base para el formulario
        model.addAttribute("productosBase", productoService.obtenerProductosBase());
        // Agregar el cliente actual al modelo para control de visibilidad
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Cliente cliente = productoService.obtenerClientePorId(clienteId);
        model.addAttribute("cliente", cliente);
        return "nuevo-producto";
    }

    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto, RedirectAttributes redirectAttributes,
            org.springframework.ui.Model model) {
        try {
            // Asignar el cliente actual usando TenantContext
            Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
            com.alocode.model.Cliente cliente = productoService.obtenerClientePorId(clienteId);
            producto.setCliente(cliente);
            System.out.println("[DEBUG] Guardando producto: " + producto);
            productoService.guardarProducto(producto);
            redirectAttributes.addFlashAttribute("success", "Producto guardado exitosamente");
            return "redirect:/productos";
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            // Volver a la vista con los datos llenados y el error
            model.addAttribute("producto", producto);
            model.addAttribute("productosBase", productoService.obtenerProductosBase());
            model.addAttribute("error", e.getMessage());
            return "nuevo-producto";
        }
    }

    @PostMapping("/desactivar/{id}")
    public String desactivarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productoService.desactivarProducto(id);
            redirectAttributes.addFlashAttribute("success", "Producto desactivado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarProducto(@PathVariable Long id, Model model) {
        model.addAttribute("producto", productoService.obtenerProductoPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado")));

        // Agregar lista de productos base para el formulario
        model.addAttribute("productosBase", productoService.obtenerProductosBase());

        return "nuevo-producto";
    }

    @PostMapping("/activar/{id}")
    public String activarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productoService.activarProducto(id);
            redirectAttributes.addFlashAttribute("success", "Producto activado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }

}