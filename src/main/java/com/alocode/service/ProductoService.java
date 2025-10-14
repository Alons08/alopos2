package com.alocode.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alocode.model.Producto;
import com.alocode.repository.ProductoRepository;
import com.alocode.repository.ClienteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    public com.alocode.model.Cliente obtenerClientePorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }
    
        // Métodos para paginación y ordenamiento
    public Page<Producto> obtenerProductosPaginados(Pageable pageable) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return productoRepository.findAllByClienteId(clienteId, pageable);
    }

    public Page<Producto> buscarProductosPorNombrePaginado(String nombre, Pageable pageable) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return productoRepository.buscarPorNombre(nombre, clienteId, pageable);
    }

    public Optional<Producto> obtenerProductoPorId(Long id) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Optional<Producto> producto = productoRepository.findById(id);
        if (producto.isPresent() && producto.get().getCliente().getId().equals(clienteId)) {
            return producto;
        }
        return Optional.empty();
    }

    public Producto guardarProducto(Producto producto) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        System.out.println("[DEBUG] Cliente actual: " + clienteId);
        System.out.println("[DEBUG] Producto recibido: " + producto);
        if (producto.getCliente() == null || !producto.getCliente().getId().equals(clienteId)) {
            System.out.println("[ERROR] El producto no tiene el cliente correcto");
            throw new IllegalArgumentException("El producto debe pertenecer al cliente actual");
        }
        // Validación de duplicados por nombre (ignorando mayúsculas/minúsculas) para el cliente
        Optional<Producto> existente = productoRepository.findByNombreIgnoreCaseAndClienteId(producto.getNombre(), clienteId);
        if (existente.isPresent() && (producto.getId() == null || !existente.get().getId().equals(producto.getId()))) {
            throw new IllegalArgumentException("Ya existe un producto con este nombre en esta empresa.");
        }
        if (producto.getProductoBase() != null) {
            if (producto.getFactorConversion() == null) {
                System.out.println("[ERROR] Falta factor de conversión");
                throw new IllegalArgumentException("Los productos derivados deben tener un factor de conversión");
            }
            producto.setStock(0.0);
            producto.setStockOcupado(0.0);
        }
        Producto guardado = productoRepository.save(producto);
        System.out.println("[DEBUG] Producto guardado: " + guardado);
        return guardado;
    }

    public void eliminarProducto(Long id) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Optional<Producto> producto = productoRepository.findById(id);
        if (producto.isPresent() && producto.get().getCliente().getId().equals(clienteId)) {
            productoRepository.deleteById(id);
        }
    }

    public void desactivarProducto(Long id) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Optional<Producto> productoOpt = productoRepository.findById(id);
        if (productoOpt.isPresent() && productoOpt.get().getCliente().getId().equals(clienteId)) {
            Producto producto = productoOpt.get();
            producto.setActivo(false);
            productoRepository.save(producto);
        }
    }

    public List<Producto> obtenerProductosActivos() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return productoRepository.findByClienteIdAndActivoTrue(clienteId);
    }

    public void activarProducto(Long id) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Optional<Producto> productoOpt = productoRepository.findById(id);
        if (productoOpt.isPresent() && productoOpt.get().getCliente().getId().equals(clienteId)) {
            Producto producto = productoOpt.get();
            producto.setActivo(true);
            productoRepository.save(producto);
        }
    }

    public List<Producto> obtenerProductosBase() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return productoRepository.findByClienteIdAndEsProductoBaseTrueAndActivoTrue(clienteId);
    }

    public List<Producto> obtenerProductosDerivados() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return productoRepository.findByClienteIdAndProductoBaseIsNotNullAndActivoTrue(clienteId);
    }
    
    public boolean verificarStockDisponible(Producto producto, Integer cantidad) {
        if (producto.getProductoBase() != null) {
            // Producto derivado - verificar stock del producto base
            Producto productoBase = producto.getProductoBase();
            double cantidadBaseNecesaria = cantidad * producto.getFactorConversion();
            return (productoBase.getStock() - productoBase.getStockOcupado()) >= cantidadBaseNecesaria;
        } else {
            // Producto normal - verificar su propio stock
            return (producto.getStock() - producto.getStockOcupado()) >= cantidad;
        }
    }
    
    @Transactional
    public void reservarStock(Producto producto, Integer cantidad) {
        if (producto.getProductoBase() != null) {
            // Producto derivado - reservar stock del producto base
            Producto productoBase = producto.getProductoBase();
            double cantidadBaseReservar = cantidad * producto.getFactorConversion();
            if ((productoBase.getStock() - productoBase.getStockOcupado()) < cantidadBaseReservar) {
                throw new IllegalStateException("Stock insuficiente del producto base: " + productoBase.getNombre());
            }
            productoBase.setStockOcupado(productoBase.getStockOcupado() + cantidadBaseReservar);
            productoRepository.save(productoBase);
        } else {
            // Producto normal - reservar su propio stock
            if ((producto.getStock() - producto.getStockOcupado()) < cantidad) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            producto.setStockOcupado(producto.getStockOcupado() + cantidad);
            productoRepository.save(producto);
        }
    }
    
    @Transactional
    public void liberarStockReservado(Producto producto, Integer cantidad) {
        if (producto.getProductoBase() != null) {
            // Producto derivado - liberar stock del producto base
            Producto productoBase = producto.getProductoBase();
            double cantidadBaseLiberar = cantidad * producto.getFactorConversion();
            productoBase.setStockOcupado(Math.max(0.0, productoBase.getStockOcupado() - cantidadBaseLiberar));
            productoRepository.save(productoBase);
        } else {
            // Producto normal - liberar su propio stock
            producto.setStockOcupado(Math.max(0.0, producto.getStockOcupado() - cantidad));
            productoRepository.save(producto);
        }
    }
    
    @Transactional
    public void consumirStock(Producto producto, Integer cantidad) {
        if (producto.getProductoBase() != null) {
            // Producto derivado - consumir stock del producto base
            Producto productoBase = producto.getProductoBase();
            double cantidadBaseConsumir = cantidad * producto.getFactorConversion();
            // Liberar primero el stock reservado
            productoBase.setStockOcupado(Math.max(0.0, productoBase.getStockOcupado() - cantidadBaseConsumir));
            // Reducir el stock real
            productoBase.setStock(Math.max(0.0, productoBase.getStock() - cantidadBaseConsumir));
            productoRepository.save(productoBase);
        } else {
            // Producto normal - consumir su propio stock
            producto.setStockOcupado(Math.max(0.0, producto.getStockOcupado() - cantidad));
            producto.setStock(Math.max(0.0, producto.getStock() - cantidad));
            productoRepository.save(producto);
        }
    }

    public long contarProductosActivos() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return productoRepository.countByClienteIdAndActivoTrue(clienteId);
    }

    public long contarProductosInactivos() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return productoRepository.countByClienteIdAndActivoFalse(clienteId);
    }
}