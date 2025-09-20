package com.alocode.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "clientes")
@Data
public class Cliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;
    
    @NotBlank(message = "El RUC es obligatorio")
    @Size(max = 20, message = "El RUC no puede exceder los 20 caracteres")
    @Column(unique = true)
    private String ruc;
    
    @NotBlank(message = "El email es obligatorio")
    @Size(max = 100, message = "El email no puede exceder los 100 caracteres")
    private String email;
    
    private String telefono;
    private String direccion;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_registro")
    private Date fechaRegistro;
    
    private Boolean activo = true;
    
    @PrePersist
    protected void onCreate() {
        fechaRegistro = new Date();
    }
}