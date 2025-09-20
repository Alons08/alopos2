package com.alocode.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuario_activo", columnList = "activo"),
    @Index(name = "idx_usuario_cliente", columnList = "cliente_id") // Nuevo índice para multi-tenancy
})
@Data
public class Usuario {

    @Id
    @Column(name = "id_usuario")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 100, message = "El nombre de usuario no puede exceder los 100 caracteres")
    @Column(unique = true)
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
    private Boolean activo = true;

    // estos 2 atributos que están a continuación funcionan con los 2 "Listeners" y "CustomAuthenticationFailureHandler"
    @Column(name = "intentos_fallidos")
    private int intentosFallidos = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_ultimo_intento")
    private Date fechaUltimoIntento;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_creacion")
    private Date fechaCreacion;

    // Relación con cliente para multi-tenancy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    //HashSet porque puede tener un conjunto de roles (varios roles)
    //Set porque no pueden haber elementos repetidos, o sea los roles son únicos
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    private Set<Rol> roles = new HashSet<>();

    
    public Usuario() {
    }

    // Constructor adicional
    public Usuario(Long id) {
        this.id = id;
    }
}