package com.alocode.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alocode.model.Caja;
import com.alocode.model.DetallePedido;
import com.alocode.model.Mesa;
import com.alocode.model.Pedido;
import com.alocode.model.Producto;
import com.alocode.model.Usuario;
import com.alocode.model.enums.EstadoMesa;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.model.enums.TipoPedido;
import com.alocode.repository.CajaRepository;
import com.alocode.repository.MesaRepository;
import com.alocode.repository.PedidoRepository;
import com.alocode.repository.ProductoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final MesaRepository mesaRepository;
    private final CajaRepository cajaRepository;
    private final ProductoService productoService;

    @Transactional
    public Pedido crearPedido(Pedido pedido, List<DetallePedido> detalles, Usuario usuario) {
        // Validar caja abierta
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Caja caja = cajaRepository.findCajaAbiertaHoy(clienteId)
            .orElseThrow(() -> new IllegalStateException("No hay caja abierta hoy"));
    // Asignar cliente al pedido
    com.alocode.model.Cliente cliente = new com.alocode.model.Cliente();
    cliente.setId(clienteId);
    pedido.setCliente(cliente);

    // Asignar número de pedido secuencial por cliente
    Integer maxNumero = pedidoRepository.findMaxNumeroPedidoByClienteId(clienteId);
    pedido.setNumeroPedido(maxNumero + 1);

        // Validar y procesar detalles
        for (DetallePedido detalle : detalles) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

            // Validar stock usando el servicio de productos
            if (!productoService.verificarStockDisponible(producto, detalle.getCantidad())) {
                String productoNombre = producto.getProductoBase() != null ? 
                    producto.getProductoBase().getNombre() : producto.getNombre();
                throw new IllegalStateException("Stock insuficiente para el producto: " + productoNombre);
            }

            // Reservar stock usando el servicio de productos
            productoService.reservarStock(producto, detalle.getCantidad());

            // Configurar el detalle del pedido
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio() * detalle.getCantidad());
            detalle.setPedido(pedido);
            
            // Para productos derivados, registrar el producto base y la cantidad consumida
            if (producto.getProductoBase() != null) {
                detalle.setProductoBase(producto.getProductoBase());
                detalle.setCantidadBaseConsumida(detalle.getCantidad() * producto.getFactorConversion());
            }
        }

        // Calcular total
        double subtotal = detalles.stream().mapToDouble(DetallePedido::getSubtotal).sum();
        pedido.setTotal(subtotal + pedido.getRecargo());

        // Asignar caja y usuario
        pedido.setCaja(caja);
        pedido.setUsuario(usuario);
        pedido.setFecha(new Date());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        // Si es pedido en mesa, actualizar estado de la mesa
        if (pedido.getTipo() == TipoPedido.MESA && pedido.getMesa() != null) {
            Mesa mesa = mesaRepository.findById(pedido.getMesa().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
            mesa.setEstado(EstadoMesa.OCUPADA);
            mesaRepository.save(mesa);
        }

        pedido.setDetalles(detalles);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido actualizarEstadoPedido(Long idPedido, EstadoPedido nuevoEstado, Usuario usuario) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        // Validar transición de estado
        if (pedido.getEstado() != EstadoPedido.PENDIENTE &&
            pedido.getEstado() != EstadoPedido.PREPARANDO &&
            pedido.getEstado() != EstadoPedido.ENTREGANDO) {
                throw new IllegalStateException("No se puede cambiar el estado de un pedido pagado o cancelado");
        }

        // Procesar cambio de estado
        if (nuevoEstado == EstadoPedido.PAGADO) {
            // Consumir stock real
            for (DetallePedido detalle : pedido.getDetalles()) {
                Producto producto = detalle.getProducto();
                productoService.consumirStock(producto, detalle.getCantidad());
            }

            // Si es pedido en mesa, liberar mesa
            if (pedido.getTipo() == TipoPedido.MESA && pedido.getMesa() != null) {
                Mesa mesa = pedido.getMesa();
                mesa.setEstado(EstadoMesa.DISPONIBLE);
                mesaRepository.save(mesa);
            }

            pedido.setFechaPagado(new Date());
            pedido.setUsuarioPagado(usuario);
        } else if (nuevoEstado == EstadoPedido.CANCELADO) {
            // Solo liberar stock reservado
            for (DetallePedido detalle : pedido.getDetalles()) {
                Producto producto = detalle.getProducto();
                productoService.liberarStockReservado(producto, detalle.getCantidad());
            }

            // Si es pedido en mesa, liberar mesa
            if (pedido.getTipo() == TipoPedido.MESA && pedido.getMesa() != null) {
                Mesa mesa = pedido.getMesa();
                mesa.setEstado(EstadoMesa.DISPONIBLE);
                mesaRepository.save(mesa);
            }
        }

        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void cancelarPedidosPendientesDeDiasAnteriores() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        List<Pedido> pedidos = pedidoRepository.findPedidosPendientesDeDiasAnteriores(clienteId);
        pedidos.forEach(p -> {
            p.setEstado(EstadoPedido.CANCELADO);
            p.getDetalles().forEach(d -> {
                Producto producto = d.getProducto();
                productoService.liberarStockReservado(producto, d.getCantidad());
            });
        });
        pedidoRepository.saveAll(pedidos);
    }

    public List<Pedido> obtenerPedidosPendientes() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        List<Pedido> pedidos = pedidoRepository.findByClienteIdAndEstadoIn(clienteId, List.of(
            EstadoPedido.PENDIENTE,
            EstadoPedido.PREPARANDO,
            EstadoPedido.ENTREGANDO));
        pedidos.sort(java.util.Comparator.comparingLong(Pedido::getId));
        return pedidos;
    }

    public Optional<Pedido> obtenerPedidoPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    @Transactional
    public void actualizarDetallesPedido(Pedido pedido, List<Long> productos, List<Integer> cantidades,
            Usuario usuario) {
        if (pedido.getEstado() == EstadoPedido.PAGADO) {
            throw new IllegalStateException("No se puede editar un pedido PAGADO");
        }
        
        // Liberar stock ocupado de los productos actuales
        for (DetallePedido detalle : pedido.getDetalles()) {
            Producto producto = detalle.getProducto();
            productoService.liberarStockReservado(producto, detalle.getCantidad());
        }
        
        pedido.getDetalles().clear();
        
        // Agregar los nuevos detalles
        for (int i = 0; i < productos.size(); i++) {
            Producto producto = productoRepository.findById(productos.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            
            // Validar stock usando el servicio de productos
            if (!productoService.verificarStockDisponible(producto, cantidades.get(i))) {
                String productoNombre = producto.getProductoBase() != null ? 
                    producto.getProductoBase().getNombre() : producto.getNombre();
                throw new IllegalStateException("Stock insuficiente para el producto: " + productoNombre);
            }
            
            // Reservar stock usando el servicio de productos
            productoService.reservarStock(producto, cantidades.get(i));
            
            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidades.get(i));
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio() * cantidades.get(i));
            detalle.setPedido(pedido);
            
            // Para productos derivados, registrar el producto base y la cantidad consumida
            if (producto.getProductoBase() != null) {
                detalle.setProductoBase(producto.getProductoBase());
                detalle.setCantidadBaseConsumida(cantidades.get(i) * producto.getFactorConversion());
            }
            
            pedido.getDetalles().add(detalle);
        }
        
        // Recalcular total
        double subtotal = pedido.getDetalles().stream().mapToDouble(DetallePedido::getSubtotal).sum();
        pedido.setTotal(subtotal + pedido.getRecargo());
        pedidoRepository.save(pedido);
    }

    public List<Pedido> obtenerPedidosPorCajaYEstado(Long cajaId, EstadoPedido estado) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return pedidoRepository.findByClienteIdAndCajaIdAndEstado(clienteId, cajaId, estado);
    }

    @Transactional
    public void actualizarPedidoYMesas(Pedido pedidoEditado, List<Long> productos, List<Integer> cantidades, Usuario usuario) {
        Pedido pedidoOriginal = pedidoRepository.findById(pedidoEditado.getId())
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        
        // Si el tipo era MESA y ahora ya no es, liberar la mesa
        if (pedidoOriginal.getTipo() == TipoPedido.MESA && pedidoEditado.getTipo() != TipoPedido.MESA) {
            Long idMesaOriginal = pedidoOriginal.getMesa() != null ? pedidoOriginal.getMesa().getId() : null;
            if (idMesaOriginal != null) {
                Mesa mesaOriginal = mesaRepository.findById(idMesaOriginal).orElse(null);
                if (mesaOriginal != null) {
                    mesaOriginal.setEstado(EstadoMesa.DISPONIBLE);
                    mesaRepository.save(mesaOriginal);
                }
            }
        }
        
        // Si el tipo era distinto de MESA y ahora es MESA, ocupar la nueva mesa
        if (pedidoOriginal.getTipo() != TipoPedido.MESA && pedidoEditado.getTipo() == TipoPedido.MESA) {
            Long idMesaNueva = pedidoEditado.getMesa() != null ? pedidoEditado.getMesa().getId() : null;
            if (idMesaNueva != null) {
                Mesa mesaNueva = mesaRepository.findById(idMesaNueva).orElse(null);
                if (mesaNueva != null) {
                    mesaNueva.setEstado(EstadoMesa.OCUPADA);
                    mesaRepository.save(mesaNueva);
                }
            }
        }
        
        // Si el tipo es MESA y la mesa cambió, liberar la anterior y ocupar la nueva
        if (pedidoOriginal.getTipo() == TipoPedido.MESA && pedidoEditado.getTipo() == TipoPedido.MESA) {
            Long idMesaOriginal = pedidoOriginal.getMesa() != null ? pedidoOriginal.getMesa().getId() : null;
            Long idMesaNueva = pedidoEditado.getMesa() != null ? pedidoEditado.getMesa().getId() : null;
            
            if (idMesaOriginal != null && !idMesaOriginal.equals(idMesaNueva)) {
                Mesa mesaOriginal = mesaRepository.findById(idMesaOriginal).orElse(null);
                if (mesaOriginal != null) {
                    mesaOriginal.setEstado(EstadoMesa.DISPONIBLE);
                    mesaRepository.save(mesaOriginal);
                }
            }
            
            if (idMesaNueva != null && !idMesaNueva.equals(idMesaOriginal)) {
                Mesa mesaNueva = mesaRepository.findById(idMesaNueva).orElse(null);
                if (mesaNueva != null) {
                    mesaNueva.setEstado(EstadoMesa.OCUPADA);
                    mesaRepository.save(mesaNueva);
                }
            }
        }
        
        // Actualizar detalles y otros campos
        pedidoOriginal.setTipo(pedidoEditado.getTipo());
        pedidoOriginal.setMesa(pedidoEditado.getMesa());
        pedidoOriginal.setRecargo(pedidoEditado.getRecargo());
        pedidoOriginal.setObservaciones(pedidoEditado.getObservaciones());
        
        actualizarDetallesPedido(pedidoOriginal, productos, cantidades, usuario);
    }
}