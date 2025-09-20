package com.alocode.model;

import com.alocode.model.enums.EstadoMesa;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "mesas", indexes = {
    @Index(name = "idx_mesa_estado", columnList = "estado"),
    @Index(name = "idx_mesa_cliente", columnList = "cliente_id"), // Nuevo índice para multi-tenancy
    @Index(name = "idx_mesa_numero_cliente", columnList = "numero,cliente_id") // Unique por cliente
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mesa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "El número es requerido")
    @Min(value = 1, message = "El número debe ser mayor a 0")
    @Column(nullable = false)
    private Integer numero;
    
    @NotNull(message = "La capacidad es requerida")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    private Integer capacidad;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMesa estado;
    
    @Size(max = 100, message = "La ubicación no puede exceder los 100 caracteres")
    private String ubicacion;
    
    // Relación con cliente para multi-tenancy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
}