package com.alocode.service;

import com.alocode.model.Cliente;
import com.alocode.model.Usuario;
import com.alocode.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
