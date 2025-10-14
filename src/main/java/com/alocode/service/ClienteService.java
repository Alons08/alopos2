package com.alocode.service;

import com.alocode.model.Cliente;
import com.alocode.model.Usuario;
import com.alocode.model.ClienteConfiguracion;
import com.alocode.repository.ClienteRepository;
import com.alocode.repository.ClienteConfiguracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ClienteConfiguracionRepository clienteConfiguracionRepository;

    public Optional<Cliente> buscarPorId(Long id) {
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
            // Actualizar configuración solo si los campos no son null
            if (datosActualizados.getConfiguracion() != null) {
                ClienteConfiguracion config = cliente.getConfiguracion();
                ClienteConfiguracion datosConfig = datosActualizados.getConfiguracion();
                if (config != null && datosConfig != null) {
                    Boolean permitir = datosConfig.getPermitirProductosDerivados();
                    Boolean mostrar = datosConfig.getMostrarEstadosPreparandoEntregando();
                    if (permitir != null) {
                        config.setPermitirProductosDerivados(permitir);
                    }
                    if (mostrar != null) {
                        config.setMostrarEstadosPreparandoEntregando(mostrar);
                    }
                    clienteConfiguracionRepository.save(config);
                }
            }
            clienteRepository.save(cliente);
            return true;
        }
        return false;
    }


}
