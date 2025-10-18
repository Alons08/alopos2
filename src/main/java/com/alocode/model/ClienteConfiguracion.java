package com.alocode.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cliente_configuracion")
@Data
public class ClienteConfiguracion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Configuración: productos derivados
    @Column(name = "permitir_productos_derivados")
    private Boolean permitirProductosDerivados = true;

    // Configuración: mostrar estados PREPARANDO y ENTREGANDO
    @Column(name = "mostrar_estados_preparando_entregando")
    private Boolean mostrarEstadosPreparandoEntregando = true;

    // Relación uno a uno con Cliente
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    @lombok.ToString.Exclude
    private Cliente cliente;
}