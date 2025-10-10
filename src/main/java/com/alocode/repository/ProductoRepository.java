package com.alocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alocode.model.Producto;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Para paginación y ordenamiento
    Page<Producto> findAllByClienteId(Long clienteId, Pageable pageable);
    
    List<Producto> findByClienteIdAndActivoTrue(Long clienteId);
    
    Optional<Producto> findByNombreIgnoreCaseAndClienteId(String nombre, Long clienteId);
    
    @Query("SELECT p FROM Producto p WHERE p.activo = TRUE AND p.cliente.id = :clienteId AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    Page<Producto> buscarPorNombre(@Param("nombre") String nombre, @Param("clienteId") Long clienteId, Pageable pageable);
    
    List<Producto> findByClienteIdAndEsProductoBaseTrueAndActivoTrue(Long clienteId);
    
    List<Producto> findByClienteIdAndProductoBaseIsNotNullAndActivoTrue(Long clienteId);
    
    List<Producto> findByClienteIdAndProductoBaseId(Long clienteId, Long productoBaseId);
}