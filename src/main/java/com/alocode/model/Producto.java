package com.alocode.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "productos", indexes = {
    @Index(name = "idx_producto_nombre", columnList = "nombre"),
    @Index(name = "idx_producto_activo", columnList = "activo"),
    @Index(name = "idx_producto_cliente", columnList = "cliente_id") // Nuevo índice para multi-tenancy
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;
    
    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
    private Double precio;
    
    @NotNull(message = "El stock es requerido")
    @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
    private Double stock;
    
    @Column(name = "stock_ocupado")
    private Double stockOcupado = 0.0;
    
    @NotNull(message = "El estado activo es requerido")
    private Boolean activo = true;
    
    @Column(name = "es_producto_base")
    private Boolean esProductoBase = false;
    
    // Relación con cliente para multi-tenancy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    // Relación con producto base (para productos derivados)
    @ManyToOne
    @JoinColumn(name = "producto_base_id")
    private Producto productoBase;
    
    // Factor de conversión (ej: 0.25 para cuarto de pollo)
    @Column(name = "factor_conversion")
    private Double factorConversion = 1.0;
}