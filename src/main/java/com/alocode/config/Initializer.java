package com.alocode.config;

import com.alocode.model.*;
import com.alocode.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.*;

@Component
public class Initializer {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {

        Cliente cliente2 = clienteRepository.findByRuc("20987654321");
        if (cliente2 == null) {
            cliente2 = new Cliente();
            cliente2.setNombre("Cafetería Dulce Aroma");
            cliente2.setRuc("20987654321");
            cliente2.setEmail("info@dulcearoma.com");
            cliente2.setTelefono("988777666");
            cliente2.setDireccion("Calle Secundaria 456");
            cliente2.setActivo(true);
            cliente2 = clienteRepository.save(cliente2);
        }

        // Crear roles globales únicos solo si no existen
        Optional<Rol> optRolAdmin = rolRepository.findByNombre("ADMIN");
        Rol rolAdmin;
        if (optRolAdmin.isEmpty()) {
            rolAdmin = new Rol();
            rolAdmin.setNombre("ADMIN");
            rolAdmin.setDescripcion("Administrador general");
            rolAdmin = rolRepository.save(rolAdmin);
        } else {
            rolAdmin = optRolAdmin.get();
        }

        Optional<Rol> optRolSecretaria = rolRepository.findByNombre("SECRETARIA");
        Rol rolSecretaria;
        if (optRolSecretaria.isEmpty()) {
            rolSecretaria = new Rol();
            rolSecretaria.setNombre("SECRETARIA");
            rolSecretaria.setDescripcion("Secretaria del local");
            rolSecretaria = rolRepository.save(rolSecretaria);
        } else {
            rolSecretaria = optRolSecretaria.get();
        }

        // Crear usuarios para cliente2 solo si no existen
        Usuario usuario3 = usuarioRepository.findByUsername("carlosruiz");
        if (usuario3 == null) {
            usuario3 = new Usuario();
            usuario3.setNombre("Carlos Ruiz");
            usuario3.setUsername("carlosruiz");
            usuario3.setPassword(passwordEncoder.encode("abcdef"));
            usuario3.setActivo(true);
            usuario3.setCliente(cliente2);
            usuario3.getRoles().add(rolAdmin);
            usuarioRepository.save(usuario3);
        }

        Usuario usuario4 = usuarioRepository.findByUsername("luciagomez");
        if (usuario4 == null) {
            usuario4 = new Usuario();
            usuario4.setNombre("Lucía Gómez");
            usuario4.setUsername("luciagomez");
            usuario4.setPassword(passwordEncoder.encode("fedcba"));
            usuario4.setActivo(true);
            usuario4.setCliente(cliente2);
            usuario4.getRoles().add(rolSecretaria);
            usuarioRepository.save(usuario4);
        }

            // Crear tercer cliente y usuarios
            Cliente cliente3 = clienteRepository.findByRuc("20543219876");
            if (cliente3 == null) {
                cliente3 = new Cliente();
                cliente3.setNombre("Pizzería La Italiana");
                cliente3.setRuc("20543219876");
                cliente3.setEmail("contacto@laitaliana.com");
                cliente3.setTelefono("977665544");
                cliente3.setDireccion("Av. Italia 789");
                cliente3.setActivo(true);
                cliente3 = clienteRepository.save(cliente3);
            }

            Usuario usuario5 = usuarioRepository.findByUsername("marcosrojas");
            if (usuario5 == null) {
                usuario5 = new Usuario();
                usuario5.setNombre("Marcos Rojas");
                usuario5.setUsername("marcosrojas");
                usuario5.setPassword(passwordEncoder.encode("v"));
                usuario5.setActivo(true);
                usuario5.setCliente(cliente3);
                usuario5.getRoles().add(rolAdmin);
                usuarioRepository.save(usuario5);
            }

            Usuario usuario6 = usuarioRepository.findByUsername("patriciasalas");
            if (usuario6 == null) {
                usuario6 = new Usuario();
                usuario6.setNombre("Patricia Salas");
                usuario6.setUsername("patriciasalas");
                usuario6.setPassword(passwordEncoder.encode("italiana321"));
                usuario6.setActivo(true);
                usuario6.setCliente(cliente3);
                usuario6.getRoles().add(rolSecretaria);
                usuarioRepository.save(usuario6);
            }
    }
}
