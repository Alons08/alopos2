package com.alocode.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alocode.model.Cliente;
import com.alocode.model.DetallePedido;
import com.alocode.model.Mesa;
import com.alocode.model.Pedido;
import com.alocode.model.Producto;
import com.alocode.model.Usuario;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.model.enums.TipoPedido;
import com.alocode.service.*;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.List;

@Controller
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService pedidoService;
    private final ProductoService productoService;
    private final MesaService mesaService;
    private final CajaService cajaService;

    @GetMapping
    public String listarPedidos(Model model) {
        List<Pedido> pedidos = pedidoService.obtenerPedidosPendientes();
        model.addAttribute("pedidos", pedidos);
        // Agregar el cliente actual al modelo para control de visibilidad de estados
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Cliente cliente = productoService.obtenerClientePorId(clienteId);
        model.addAttribute("cliente", cliente);
        return "pedidos";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoPedido(Model model) {
        boolean cajaAbierta = cajaService.obtenerCajaAbiertaHoy().isPresent();
        model.addAttribute("cajaAbierta", cajaAbierta);
        Pedido pedido = new Pedido();
        pedido.setMesa(new Mesa()); // Mesa vacía para que mesa.id sea null
        model.addAttribute("pedido", pedido);
        model.addAttribute("tiposPedido", TipoPedido.values());
        model.addAttribute("mesasDisponibles", mesaService.obtenerMesasDisponibles());
        model.addAttribute("productos", productoService.obtenerProductosActivos());
        return "nuevo-pedido";
    }

    @PostMapping("/guardar")
    public String guardarPedido(@ModelAttribute Pedido pedido,
            @RequestParam List<Long> productos,
            @RequestParam List<Integer> cantidades,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        System.out.println("--- [LOG] Guardar Pedido ---");
        System.out.println("Pedido recibido: " + pedido);
        System.out.println("Productos: " + productos);
        System.out.println("Cantidades: " + cantidades);
        Usuario usuario = null;
        if (userDetails instanceof MyUserDetails myUserDetails) {
            usuario = myUserDetails.getUsuario();
        }
        System.out.println("Usuario: " + (usuario != null ? usuario.getUsername() : "null"));
        try {
            // Validar caja abierta
            if (!cajaService.obtenerCajaAbiertaHoy().isPresent()) {
                System.out.println("[LOG] No hay caja abierta hoy");
                redirectAttributes.addFlashAttribute("error", "No hay caja abierta hoy");
                return "redirect:/pedidos/nuevo";
            }

            if (pedido.getId() != null) {
                // Es edición
                pedidoService.actualizarPedidoYMesas(pedido, productos, cantidades, usuario);
                redirectAttributes.addFlashAttribute("success", "Pedido actualizado exitosamente");
            } else {
                // Es nuevo
                // Crear detalles del pedido
                for (int i = 0; i < productos.size(); i++) {
                    Producto producto = productoService.obtenerProductoPorId(productos.get(i))
                            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

                    DetallePedido detalle = new DetallePedido();
                    detalle.setProducto(producto);
                    detalle.setCantidad(cantidades.get(i));
                    detalle.setPrecioUnitario(producto.getPrecio());
                    detalle.setSubtotal(producto.getPrecio() * cantidades.get(i));
                    pedido.getDetalles().add(detalle);
                    System.out.println(
                            "[LOG] Detalle agregado: Producto=" + producto.getNombre() + ", Cantidad="
                                    + cantidades.get(i));
                }
                if (usuario == null) {
                    throw new IllegalStateException("No se pudo obtener el usuario autenticado");
                }
                pedidoService.crearPedido(pedido, pedido.getDetalles(), usuario);
                redirectAttributes.addFlashAttribute("success", "Pedido creado exitosamente");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos/nuevo";
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstadoPedido(@PathVariable Long id,
            @RequestParam EstadoPedido estado,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = null;
        if (userDetails instanceof MyUserDetails myUserDetails) {
            usuario = myUserDetails.getUsuario();
        }
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "No se pudo obtener el usuario autenticado");
            return "redirect:/pedidos";
        }
        try {
            pedidoService.actualizarEstadoPedido(id, estado, usuario);
            redirectAttributes.addFlashAttribute("success", "Estado del pedido actualizado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }

    @GetMapping("/{id}/detalle")
    public String verDetallePedido(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return pedidoService.obtenerPedidoPorId(id)
                .map(pedido -> {
                    model.addAttribute("pedido", pedido);
                    // Agregar el cliente actual al modelo para visibilidad de estados
                    if (pedido.getCliente() != null) {
                        model.addAttribute("cliente", pedido.getCliente());
                    }
                    return "detalle-pedido";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Pedido no encontrado");
                    return "redirect:/pedidos";
                });
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditarPedido(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        return pedidoService.obtenerPedidoPorId(id)
                .map(pedido -> {
                    if (pedido.getEstado() == EstadoPedido.PAGADO) {
                        redirectAttributes.addFlashAttribute("error", "No se puede editar un pedido PAGADO");
                        return "redirect:/pedidos";
                    }
                    // Obtener mesas disponibles y agregar la mesa asignada si no está
                    List<com.alocode.model.Mesa> mesasDisponibles = mesaService.obtenerMesasDisponibles();
                    if (pedido.getMesa() != null && pedido.getMesa().getId() != null) {
                        boolean yaIncluida = mesasDisponibles.stream()
                                .anyMatch(m -> m.getId().equals(pedido.getMesa().getId()));
                        if (!yaIncluida) {
                            mesasDisponibles.add(pedido.getMesa());
                        }
                    }
                    model.addAttribute("pedido", pedido);
                    model.addAttribute("tiposPedido", TipoPedido.values());
                    model.addAttribute("mesasDisponibles", mesasDisponibles);
                    model.addAttribute("productos", productoService.obtenerProductosActivos());
                    boolean cajaAbierta = cajaService.obtenerCajaAbiertaHoy().isPresent();
                    model.addAttribute("cajaAbierta", cajaAbierta);
                    return "nuevo-pedido";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Pedido no encontrado");
                    return "redirect:/pedidos";
                });
    }

    @GetMapping("/{id}/comprobante")
    public void generarComprobante(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Pedido pedido = pedidoService.obtenerPedidoPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        response.setContentType("application/pdf");
        String numeroPedidoStr = String.format("%06d", pedido.getNumeroPedido());
        Cliente cliente = pedido.getCliente();
        String serie = "B00" + cliente.getId();
        String nombreArchivo = "Comprobante_" + serie + "-" + numeroPedidoStr + ".pdf";
        response.setHeader("Content-Disposition", "inline; filename=" + nombreArchivo);

        // Tamaño comprobante típico: 80mm x 200mm (en puntos: 1 pulgada = 72 puntos)
        float width = 226.77f; // 80mm
        float height = 566.93f; // 200mm
        Document document = new Document(new Rectangle(width, height), 10, 10, 10, 10);
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // --- DATOS DE EMPRESA DINÁMICOS ---
            Font fontTitle = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font fontNormal = new Font(Font.HELVETICA, 7, Font.NORMAL);
            Font fontBold = new Font(Font.HELVETICA, 7, Font.BOLD);
            Paragraph nombreEmpresa = new Paragraph(cliente.getNombre(), fontTitle);
            nombreEmpresa.setAlignment(Element.ALIGN_CENTER);
            document.add(nombreEmpresa);
            Paragraph ruc = new Paragraph("RUC: " + cliente.getRuc(), fontNormal);
            ruc.setAlignment(Element.ALIGN_CENTER);
            document.add(ruc);
            Paragraph direccion = new Paragraph(cliente.getDireccion() != null ? cliente.getDireccion() : "",
                    fontNormal);
            direccion.setAlignment(Element.ALIGN_CENTER);
            document.add(direccion);
            document.add(new Paragraph(
                    "----------------------------------------------------------------------------------------",
                    fontNormal));
            Paragraph tipoDoc = new Paragraph("COMPROBANTE DE VENTA ELECTRÓNICA", fontBold);
            tipoDoc.setAlignment(Element.ALIGN_CENTER);
            document.add(tipoDoc);
            document.add(
                    new Paragraph("N°: " + serie + "-" + String.format("%06d", pedido.getNumeroPedido()), fontBold));
            document.add(new Paragraph(
                    "----------------------------------------------------------------------------------------",
                    fontNormal));

            // --- DATOS DE FECHA Y ATENCIÓN ---
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            document.add(new Paragraph("Fecha y Hora: " +
                    sdf.format(pedido.getFechaPagado() != null ? pedido.getFechaPagado() : pedido.getFecha()),
                    fontNormal));
            if (pedido.getMesa() != null) {
                document.add(new Paragraph("Mesa: " + pedido.getMesa().getNumero(), fontNormal));
            }
            document.add(new Paragraph(
                    "Atiende: " + (pedido.getUsuario() != null ? pedido.getUsuario().getNombre() : "-"), fontNormal));
            document.add(new Paragraph(
                    "----------------------------------------------------------------------------------------",
                    fontNormal));

            // --- TABLA DE PRODUCTOS ---
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2, 8, 3, 4 });
            table.addCell(new Phrase("Cant", fontBold));
            table.addCell(new Phrase("Descripción", fontBold));
            table.addCell(new Phrase("P.U.", fontBold));
            table.addCell(new Phrase("Importe", fontBold));
            double subtotal = 0.0;
            for (var det : pedido.getDetalles()) {
                table.addCell(new Phrase(String.valueOf(det.getCantidad()), fontNormal));
                table.addCell(new Phrase(det.getProducto().getNombre(), fontNormal));
                table.addCell(new Phrase(String.format("%.2f", det.getPrecioUnitario()), fontNormal));
                table.addCell(new Phrase(String.format("%.2f", det.getSubtotal()), fontNormal));
                subtotal += det.getSubtotal();
            }
            document.add(table);
            document.add(new Paragraph(
                    "----------------------------------------------------------------------------------------",
                    fontNormal));

            // --- TOTALES ---
            double igv = subtotal * 0.18;
            document.add(new Paragraph(String.format("SUBTOTAL:   S/ %.2f", subtotal), fontNormal));
            document.add(new Paragraph(String.format("IGV (18%%):     S/ %.2f", igv), fontNormal));
            document.add(new Paragraph(String.format("RECARGO:    S/ %.2f", pedido.getRecargo()), fontNormal));
            document.add(new Paragraph(String.format("TOTAL:          S/ %.2f", pedido.getTotal()), fontBold));
            document.add(new Paragraph(
                    "----------------------------------------------------------------------------------------",
                    fontNormal));

            document.add(new Paragraph(" "));
            Paragraph gracias = new Paragraph("¡Gracias por su compra!", fontNormal);
            gracias.setAlignment(Element.ALIGN_CENTER);
            document.add(gracias);
        } finally {
            document.close();
        }
    }

        @PostMapping("/{id}/comprobante-pagado")
        public void generarComprobanteYMarcarPagado(@PathVariable Long id, HttpServletResponse response, @AuthenticationPrincipal UserDetails userDetails) throws IOException {
            try {
                Pedido pedido = pedidoService.obtenerPedidoPorId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

                // Cambiar estado a PAGADO si no lo está
                if (pedido.getEstado() != EstadoPedido.PAGADO) {
                    Usuario usuario = null;
                    if (userDetails instanceof MyUserDetails myUserDetails) {
                        usuario = myUserDetails.getUsuario();
                    }
                    if (usuario == null) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("text/plain");
                        response.getWriter().write("Usuario no autenticado");
                        return;
                    }
                    pedidoService.actualizarEstadoPedido(id, EstadoPedido.PAGADO, usuario);
                    // Recargar el pedido actualizado
                    pedido = pedidoService.obtenerPedidoPorId(id).orElse(pedido);
                }

                response.setContentType("application/pdf");
                String numeroPedidoStr = String.format("%06d", pedido.getNumeroPedido());
                Cliente cliente = pedido.getCliente();
                String serie = "B00" + cliente.getId();
                String nombreArchivo = "Comprobante_" + serie + "-" + numeroPedidoStr + ".pdf";
                response.setHeader("Content-Disposition", "inline; filename=" + nombreArchivo);

                float width = 226.77f; // 80mm
                float height = 566.93f; // 200mm
                Document document = new Document(new Rectangle(width, height), 10, 10, 10, 10);
                try {
                    PdfWriter.getInstance(document, response.getOutputStream());
                    document.open();
                    Font fontTitle = new Font(Font.HELVETICA, 11, Font.BOLD);
                    Font fontNormal = new Font(Font.HELVETICA, 7, Font.NORMAL);
                    Font fontBold = new Font(Font.HELVETICA, 7, Font.BOLD);
                    Paragraph nombreEmpresa = new Paragraph(cliente.getNombre(), fontTitle);
                    nombreEmpresa.setAlignment(Element.ALIGN_CENTER);
                    document.add(nombreEmpresa);
                    Paragraph ruc = new Paragraph("RUC: " + cliente.getRuc(), fontNormal);
                    ruc.setAlignment(Element.ALIGN_CENTER);
                    document.add(ruc);
                    Paragraph direccion = new Paragraph(cliente.getDireccion() != null ? cliente.getDireccion() : "", fontNormal);
                    direccion.setAlignment(Element.ALIGN_CENTER);
                    document.add(direccion);
                    document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));
                    Paragraph tipoDoc = new Paragraph("COMPROBANTE DE VENTA ELECTRÓNICA", fontBold);
                    tipoDoc.setAlignment(Element.ALIGN_CENTER);
                    document.add(tipoDoc);
                    document.add(new Paragraph("N°: " + serie + "-" + String.format("%06d", pedido.getNumeroPedido()), fontBold));
                    document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                    document.add(new Paragraph("Fecha y Hora: " +
                            sdf.format(pedido.getFechaPagado() != null ? pedido.getFechaPagado() : pedido.getFecha()),
                            fontNormal));
                    if (pedido.getMesa() != null) {
                        document.add(new Paragraph("Mesa: " + pedido.getMesa().getNumero(), fontNormal));
                    }
                    document.add(new Paragraph("Atiende: " + (pedido.getUsuario() != null ? pedido.getUsuario().getNombre() : "-"), fontNormal));
                    document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));
                    com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[] { 2, 8, 3, 4 });
                    table.addCell(new com.lowagie.text.Phrase("Cant", fontBold));
                    table.addCell(new com.lowagie.text.Phrase("Descripción", fontBold));
                    table.addCell(new com.lowagie.text.Phrase("P.U.", fontBold));
                    table.addCell(new com.lowagie.text.Phrase("Importe", fontBold));
                    double subtotal = 0.0;
                    for (var det : pedido.getDetalles()) {
                        table.addCell(new com.lowagie.text.Phrase(String.valueOf(det.getCantidad()), fontNormal));
                        table.addCell(new com.lowagie.text.Phrase(det.getProducto().getNombre(), fontNormal));
                        table.addCell(new com.lowagie.text.Phrase(String.format("%.2f", det.getPrecioUnitario()), fontNormal));
                        table.addCell(new com.lowagie.text.Phrase(String.format("%.2f", det.getSubtotal()), fontNormal));
                        subtotal += det.getSubtotal();
                    }
                    document.add(table);
                    document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));
                    double igv = subtotal * 0.18;
                    document.add(new Paragraph(String.format("SUBTOTAL:   S/ %.2f", subtotal), fontNormal));
                    document.add(new Paragraph(String.format("IGV (18%%):     S/ %.2f", igv), fontNormal));
                    document.add(new Paragraph(String.format("RECARGO:    S/ %.2f", pedido.getRecargo()), fontNormal));
                    document.add(new Paragraph(String.format("TOTAL:          S/ %.2f", pedido.getTotal()), fontBold));
                    document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));
                    document.add(new Paragraph(" "));
                    Paragraph gracias = new Paragraph("¡Gracias por su compra!", fontNormal);
                    gracias.setAlignment(Element.ALIGN_CENTER);
                    document.add(gracias);
                } finally {
                    document.close();
                }
            } catch (Exception ex) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain");
                response.getWriter().write("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

}