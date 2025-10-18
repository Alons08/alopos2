package com.alocode.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.alocode.repository.DetallePedidoRepository;
import com.alocode.repository.PedidoRepository;
import com.alocode.repository.ProductoRepository;
import com.alocode.model.Pedido;
import com.alocode.model.Producto;
import com.alocode.model.enums.EstadoPedido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;

    /**
     * Obtiene las ventas de los últimos 7 días
     * Retorna un mapa con fecha y total de ventas por día
     */
    public Map<String, Object> obtenerVentasUltimos7Dias(Long clienteId) {
        log.info("=== INICIO obtenerVentasUltimos7Dias para clienteId: {} ===", clienteId);
        Map<String, Object> resultado = new LinkedHashMap<>();
        List<String> fechas = new ArrayList<>();
        List<Double> totales = new ArrayList<>();

        LocalDate hoy = LocalDate.now();
        log.info("Fecha de hoy: {}", hoy);
        
        // Recorrer los últimos 7 días
        for (int i = 6; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);
            LocalDateTime inicioDelDia = fecha.atStartOfDay();
            LocalDateTime finDelDia = fecha.atTime(23, 59, 59);

            log.info("Buscando pedidos para fecha: {} (desde {} hasta {})", fecha, inicioDelDia, finDelDia);

            // Buscar pedidos pagados en ese día
            List<Pedido> pedidos = pedidoRepository.findByClienteIdAndEstadoAndFechaPagadoBetweenOrderByIdAsc(
                clienteId, 
                EstadoPedido.PAGADO, 
                inicioDelDia, 
                finDelDia
            );

            log.info("Pedidos encontrados para {}: {} pedidos", fecha, pedidos.size());
            if (!pedidos.isEmpty()) {
                pedidos.forEach(p -> log.info("  - Pedido #{} - Total: {} - FechaPagado: {}", 
                    p.getNumeroPedido(), p.getTotal(), p.getFechaPagado()));
            }

            // Calcular total del día
            double totalDia = pedidos.stream()
                .mapToDouble(p -> p.getTotal())
                .sum();

            // Formatear fecha para mostrar (ej: "11/10", "12/10")
            String fechaFormateada = String.format("%02d/%02d", fecha.getDayOfMonth(), fecha.getMonthValue());
            
            log.info("Total del día {}: S/ {}", fechaFormateada, totalDia);
            
            fechas.add(fechaFormateada);
            totales.add(totalDia);
        }

        resultado.put("fechas", fechas);
        resultado.put("totales", totales);
        
        log.info("=== FIN obtenerVentasUltimos7Dias - Total de fechas: {}, Total de montos: {} ===", 
            fechas.size(), totales.size());
        log.info("Fechas: {}", fechas);
        log.info("Totales: {}", totales);
        
        return resultado;
    }

    /**
     * Obtiene los 10 productos más vendidos del mes actual
     */
    public List<Map<String, Object>> obtenerTop10ProductosMasVendidos(Long clienteId) {
        log.info("=== INICIO obtenerTop10ProductosMasVendidos para clienteId: {} ===", clienteId);
        
        List<Object[]> resultados = detallePedidoRepository.findTop10ProductosMasVendidosDelMes(clienteId);
        
        log.info("Productos encontrados: {} productos", resultados.size());
        
        List<Map<String, Object>> top10 = resultados.stream()
            .limit(10)
            .map(row -> {
                Map<String, Object> producto = new HashMap<>();
                producto.put("nombre", row[0]); // productoNombre
                producto.put("cantidad", ((Number) row[1]).intValue()); // totalCantidad
                log.info("  - Producto: {} - Cantidad: {}", row[0], row[1]);
                return producto;
            })
            .collect(Collectors.toList());
        
        log.info("=== FIN obtenerTop10ProductosMasVendidos - Total: {} productos ===", top10.size());
        
        return top10;
    }

    /**
     * Obtiene todos los productos con stock bajo (≤ 5 unidades)
     */
    public List<Map<String, Object>> obtenerProductosStockBajo(Long clienteId) {
        log.info("=== INICIO obtenerProductosStockBajo para clienteId: {} ===", clienteId);
        
        List<Producto> productos = productoRepository.findProductosConStockBajo(clienteId, 5);
        
        log.info("Productos con stock bajo encontrados: {} productos", productos.size());
        
        List<Map<String, Object>> productosStockBajo = productos.stream()
            .map(p -> {
                Map<String, Object> producto = new HashMap<>();
                producto.put("nombre", p.getNombre());
                producto.put("stock", p.getStock());
                log.info("  - Producto: {} - Stock: {}", p.getNombre(), p.getStock());
                return producto;
            })
            .collect(Collectors.toList());
        
        log.info("=== FIN obtenerProductosStockBajo - Total: {} productos ===", productosStockBajo.size());
        
        return productosStockBajo;
    }
}
