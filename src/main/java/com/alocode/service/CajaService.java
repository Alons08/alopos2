package com.alocode.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alocode.model.Caja;
import com.alocode.model.Pedido;
import com.alocode.model.Usuario;
import com.alocode.model.enums.EstadoCaja;
import com.alocode.model.enums.EstadoMesa;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.repository.CajaRepository;
import com.alocode.repository.PedidoRepository;
import com.alocode.repository.MesaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CajaService {
    private final CajaRepository cajaRepository;
    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final com.alocode.repository.ClienteRepository clienteRepository;

    @Transactional
    public Caja abrirCaja(Double montoApertura, Usuario usuario) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Date fechaActual = new Date();
        // Verificar si ya hay una caja abierta hoy
        if (cajaRepository.findByClienteIdAndFechaAndEstado(clienteId, fechaActual, EstadoCaja.ABIERTA).isPresent()) {
            throw new IllegalStateException("Ya hay una caja abierta hoy");
        }
        // Buscar si existe alguna caja ABIERTA de días anteriores y cerrarla automáticamente
        Optional<Caja> cajaAbiertaAnterior = cajaRepository.findAll().stream()
                .filter(c -> c.getEstado() == EstadoCaja.ABIERTA && !esMismoDia(c.getFecha(), fechaActual))
                .findFirst();
        cajaAbiertaAnterior.ifPresent(caja -> {
            List<Pedido> pedidos = pedidoRepository.findByClienteIdAndCajaIdAndEstado(clienteId, caja.getId(), EstadoPedido.PAGADO);
            double totalVentas = pedidos.stream().mapToDouble(p -> p.getTotal() - p.getRecargo()).sum();
            double totalRecargos = pedidos.stream().mapToDouble(Pedido::getRecargo).sum();
            double totalNeto = totalVentas + totalRecargos;
            caja.setMontoCierre(caja.getMontoApertura() + totalNeto);
            caja.setEstado(EstadoCaja.CERRADA);
            caja.setHoraCierre(new Date());
            // Cambiar a CANCELADO los pedidos en estado PENDIENTE, PREPARANDO o ENTREGANDO
            List<Pedido> pedidosNoFinalizados = pedidoRepository.findPedidosNoFinalizadosPorCaja(caja.getId(), clienteId);
            pedidosNoFinalizados.forEach(p -> {
                p.setEstado(EstadoPedido.CANCELADO);
                if (p.getTipo() != null && p.getTipo().name().equals("MESA") && p.getMesa() != null) {
                    p.getMesa().setEstado(EstadoMesa.DISPONIBLE);
                    mesaRepository.save(p.getMesa());
                }
            });
            pedidoRepository.saveAll(pedidosNoFinalizados);
            cajaRepository.save(caja);
        });
        // Asignar el cliente actual a la caja
        com.alocode.model.Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalStateException("Cliente no encontrado para el tenant actual"));
        Caja caja = new Caja();
        caja.setFecha(fechaActual);
        caja.setMontoApertura(montoApertura);
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setUsuario(usuario);
        caja.setHoraApertura(fechaActual);
        caja.setCliente(cliente);
        return cajaRepository.save(caja);
    }

    // Método utilitario para comparar si dos fechas son el mismo día (ignorando hora)
    private boolean esMismoDia(Date fecha1, Date fecha2) {
        if (fecha1 == null || fecha2 == null)
            return false;
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(fecha1);
        cal2.setTime(fecha2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
                && cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    @Transactional
    public Caja cerrarCaja(Long idCaja, Usuario usuario) {
        Caja caja = cajaRepository.findById(idCaja)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada"));

        if (caja.getEstado() == EstadoCaja.CERRADA) {
            throw new IllegalStateException("La caja ya está cerrada");
        }

        // Calcular total de ventas
        List<Pedido> pedidos = pedidoRepository.findByClienteIdAndCajaIdAndEstado(caja.getCliente().getId(), idCaja, EstadoPedido.PAGADO);
        double totalVentas = pedidos.stream().mapToDouble(p -> p.getTotal() - p.getRecargo()).sum();
        double totalRecargos = pedidos.stream().mapToDouble(Pedido::getRecargo).sum();
        double totalNeto = totalVentas + totalRecargos;
        caja.setMontoCierre(caja.getMontoApertura() + totalNeto);
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setHoraCierre(new Date());

        // Cancelar pedidos pendientes y liberar mesas si corresponde
        List<Pedido> pedidosNoFinalizados = pedidoRepository.findPedidosNoFinalizadosPorCaja(idCaja, caja.getCliente().getId());
        pedidosNoFinalizados.forEach(p -> {
            p.setEstado(EstadoPedido.CANCELADO);
            // Liberar mesa si es pedido de mesa y tiene mesa asignada
            if (p.getTipo() != null && p.getTipo().name().equals("MESA") && p.getMesa() != null) {
                p.getMesa().setEstado(com.alocode.model.enums.EstadoMesa.DISPONIBLE);
                mesaRepository.save(p.getMesa());
            }
        });
        pedidoRepository.saveAll(pedidosNoFinalizados);
        return cajaRepository.save(caja);
    }

    public Optional<Caja> obtenerCajaAbiertaHoy() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        Optional<Caja> cajaOpt = cajaRepository.findCajaAbiertaHoy(clienteId);
        if (cajaOpt.isPresent()) {
            Caja caja = cajaOpt.get();
        } else {
        }
        return cajaOpt;
    }

}