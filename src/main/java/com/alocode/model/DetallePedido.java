package com.alocode.model;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "detalle_pedido", indexes = {
    @Index(name = "idx_detalle_pedido_pedido", columnList = "pedido_id"),
    @Index(name = "idx_detalle_pedido_producto", columnList = "producto_id"),
    @Index(name = "idx_detalle_pedido_producto_base", columnList = "producto_base_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedido {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
    
    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    // Para productos derivados, referencia al producto base consumido
    @ManyToOne
    @JoinColumn(name = "producto_base_id")
    private Producto productoBase;
    
    @Column(nullable = false)
    private Integer cantidad;
    
    @Column(nullable = false)
    private Double precioUnitario;
    
    @Column(nullable = false)
    private Double subtotal;
    
    // Cantidad consumida del producto base
    @Column(name = "cantidad_base_consumida")
    private Double cantidadBaseConsumida = 0.0;
}