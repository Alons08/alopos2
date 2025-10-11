package com.alocode.service;

import com.alocode.model.Cliente;
import com.alocode.model.Usuario;
import com.alocode.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;

    public Optional<Cliente> findById(Long id) {
        return clienteRepository.findById(id);
    }

    // Solo ADMIN puede editar su propio cliente
    public boolean editarCliente(Long clienteId, Cliente datosActualizados, Usuario usuario) {
        boolean esAdmin = usuario.getRoles().stream().anyMatch(r -> r.getNombre().equals("ADMIN"));
        if (!esAdmin) return false;
        if (!usuario.getCliente().getId().equals(clienteId)) return false;
        Optional<Cliente> optCliente = clienteRepository.findById(clienteId);
        if (optCliente.isPresent()) {
            Cliente cliente = optCliente.get();
            // Validar RUC duplicado (ignorando mayúsculas/minúsculas)
            String nuevoRuc = datosActualizados.getRuc();
            if (nuevoRuc != null && !nuevoRuc.trim().isEmpty()) {
                Cliente existente = clienteRepository.findByRuc(nuevoRuc);
                if (existente != null && !existente.getId().equals(cliente.getId()) && existente.getRuc().equalsIgnoreCase(nuevoRuc)) {
                    throw new IllegalArgumentException("Ya existe una empresa con ese RUC.");
                }
            }
            cliente.setNombre(datosActualizados.getNombre());
            cliente.setRuc(datosActualizados.getRuc());
            cliente.setEmail(datosActualizados.getEmail());
            cliente.setTelefono(datosActualizados.getTelefono());
            cliente.setDireccion(datosActualizados.getDireccion());
            clienteRepository.save(cliente);
            return true;
        }
        return false;
    }
}
