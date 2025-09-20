package com.alocode.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import com.alocode.model.enums.EstadoCaja;

@Entity
@Table(name = "caja", indexes = {
    @Index(name = "idx_caja_fecha", columnList = "fecha"),
    @Index(name = "idx_caja_estado", columnList = "estado"),
    @Index(name = "idx_caja_usuario", columnList = "usuario_id"),
    @Index(name = "idx_caja_cliente", columnList = "cliente_id") // Nuevo índice para multi-tenancy
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Caja {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Temporal(TemporalType.DATE)
    private Date fecha;
    
    @Column(nullable = false)
    private Double montoApertura;
    
    private Double montoCierre;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCaja estado;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    // Relación con cliente para multi-tenancy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    @Temporal(TemporalType.TIME)
    private Date horaApertura;
    
    @Temporal(TemporalType.TIME)
    private Date horaCierre;
}