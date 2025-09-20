package com.alocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alocode.model.Caja;
import com.alocode.model.enums.EstadoCaja;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Long> {
    
    Optional<Caja> findByClienteIdAndFechaAndEstado(Long clienteId, Date fecha, EstadoCaja estado);
    
    @Query("SELECT c FROM Caja c WHERE c.fecha = :fecha AND c.cliente.id = :clienteId ORDER BY c.id DESC")
    List<Caja> findByFecha(@Param("clienteId") Long clienteId, @Param("fecha") Date fecha);
    
    @Query("SELECT c FROM Caja c WHERE c.estado = 'ABIERTA' AND c.fecha = CURRENT_DATE AND c.cliente.id = :clienteId")
    Optional<Caja> findCajaAbiertaHoy(@Param("clienteId") Long clienteId);

    List<Caja> findByClienteIdAndFechaBetween(Long clienteId, Date inicio, Date fin);
}