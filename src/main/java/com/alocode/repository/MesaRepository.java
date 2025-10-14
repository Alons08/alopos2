package com.alocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alocode.model.Mesa;
import com.alocode.model.enums.EstadoMesa;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findAllByClienteIdOrderByIdAsc(Long clienteId);
    List<Mesa> findByClienteIdAndEstado(Long clienteId, EstadoMesa estado);
    
    @Query("SELECT m FROM Mesa m WHERE m.estado = 'DISPONIBLE' AND m.cliente.id = :clienteId ORDER BY m.numero")
    List<Mesa> findMesasDisponibles(@Param("clienteId") Long clienteId);
    
    // Cuenta mesas activas (DISPONIBLE u OCUPADA)
    @Query("SELECT COUNT(m) FROM Mesa m WHERE m.cliente.id = :clienteId AND (m.estado = 'DISPONIBLE' OR m.estado = 'OCUPADA')")
    long countByClienteIdAndActiva(@Param("clienteId") Long clienteId);

    // Cuenta mesas inactivas
    @Query("SELECT COUNT(m) FROM Mesa m WHERE m.cliente.id = :clienteId AND m.estado = 'INACTIVA'")
    long countByClienteIdAndInactiva(@Param("clienteId") Long clienteId);
    
    Optional<Mesa> findByClienteIdAndNumero(Long clienteId, Integer numero);

    @Query("SELECT m FROM Mesa m WHERE m.cliente.id = :clienteId AND (:q IS NULL OR CAST(m.numero AS string) LIKE %:q% OR LOWER(m.estado) LIKE LOWER(CONCAT('%', :q, '%')) ) ORDER BY m.numero")
    List<Mesa> buscarPorNumeroOEstado(@Param("clienteId") Long clienteId, @Param("q") String q);

}