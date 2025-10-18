package com.alocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alocode.model.DetallePedido;

import java.util.List;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    
    /**
     * Obtiene los 10 productos más vendidos del mes actual
     * Retorna una lista de mapas con: productoNombre y totalCantidad
     */
    @Query("SELECT " +
           "CASE WHEN p.productoBase IS NOT NULL THEN p.productoBase.nombre ELSE p.nombre END as productoNombre, " +
           "SUM(d.cantidad) as totalCantidad " +
           "FROM DetallePedido d " +
           "JOIN d.producto p " +
           "LEFT JOIN p.productoBase pb " +
           "JOIN d.pedido ped " +
           "WHERE ped.cliente.id = :clienteId " +
           "AND ped.estado = 'PAGADO' " +
           "AND MONTH(ped.fechaPagado) = MONTH(CURRENT_DATE) " +
           "AND YEAR(ped.fechaPagado) = YEAR(CURRENT_DATE) " +
           "GROUP BY CASE WHEN p.productoBase IS NOT NULL THEN p.productoBase.nombre ELSE p.nombre END " +
           "ORDER BY totalCantidad DESC")
    List<Object[]> findTop10ProductosMasVendidosDelMes(@Param("clienteId") Long clienteId);
}
