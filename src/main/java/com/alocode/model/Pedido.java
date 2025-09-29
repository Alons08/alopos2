package com.alocode.model;

import lombok.*;
import jakarta.persistence.*;
import java.util.Date;
import java.util.List;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.model.enums.TipoPedido;

@Entity
@Table(name = "pedidos",
    indexes = {
        @Index(name = "idx_pedido_fecha", columnList = "fecha"),
        @Index(name = "idx_pedido_estado", columnList = "estado"),
        @Index(name = "idx_pedido_usuario", columnList = "usuario_id"),
        @Index(name = "idx_pedido_caja", columnList = "caja_id"),
        @Index(name = "idx_pedido_cliente", columnList = "cliente_id") // Nuevo índice para multi-tenancy
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cliente_numero_pedido", columnNames = {"cliente_id", "numero_pedido"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_pedido", nullable = false)
    private Integer numeroPedido;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPedido tipo;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;
    
    @ManyToOne
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;
    
    @Column(nullable = false)
    private Double total;
    
    @Column(nullable = false)
    private Double recargo = 0.0;

    public Double getRecargo() {
        return recargo == null ? 0.0 : recargo;
    }

    public void setRecargo(Double recargo) {
        this.recargo = (recargo == null ? 0.0 : recargo);
    }
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @ManyToOne
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;
    
    // Relación con cliente para multi-tenancy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    @Column(length = 500)
    private String observaciones;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaPagado;
    
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new java.util.ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "usuario_pagado_id")
    private Usuario usuarioPagado;

    public Usuario getUsuarioPagado() {
        return usuarioPagado;
    }

    public void setUsuarioPagado(Usuario usuarioPagado) {
        this.usuarioPagado = usuarioPagado;
    }
}