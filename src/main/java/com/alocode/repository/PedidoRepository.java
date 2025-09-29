package com.alocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alocode.model.Pedido;
import com.alocode.model.enums.EstadoPedido;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findAllByClienteIdOrderByIdAsc(Long clienteId);
    
    List<Pedido> findByClienteIdAndEstadoIn(Long clienteId, List<EstadoPedido> estados);
    
    @Query("SELECT p FROM Pedido p WHERE p.estado = 'PENDIENTE' AND CAST(p.fecha AS date) < CURRENT_DATE AND p.cliente.id = :clienteId")
    List<Pedido> findPedidosPendientesDeDiasAnteriores(@Param("clienteId") Long clienteId);
    

    @Query("SELECT p FROM Pedido p WHERE p.caja.id = :cajaId AND p.estado IN ('PENDIENTE', 'PREPARANDO', 'ENTREGANDO') AND p.cliente.id = :clienteId")
    List<Pedido> findPedidosNoFinalizadosPorCaja(@Param("cajaId") Long cajaId, @Param("clienteId") Long clienteId);
    
    @Query("SELECT p FROM Pedido p WHERE p.estado = 'PAGADO' AND CAST(p.fechaPagado AS date) BETWEEN :inicio AND :fin AND p.cliente.id = :clienteId")
    List<Pedido> findPedidosPagadosEntreFechas(@Param("inicio") Date inicio, @Param("fin") Date fin, @Param("clienteId") Long clienteId);
    
    List<Pedido> findByClienteIdAndCajaIdAndEstado(Long clienteId, Long cajaId, EstadoPedido estado);
    
    List<Pedido> findByClienteIdAndEstadoAndFechaPagadoBetweenOrderByIdAsc(Long clienteId, EstadoPedido estado, LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT COALESCE(MAX(p.numeroPedido), 0) FROM Pedido p WHERE p.cliente.id = :clienteId")
    Integer findMaxNumeroPedidoByClienteId(@Param("clienteId") Long clienteId);
}