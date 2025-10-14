package com.alocode.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.alocode.model.Mesa;
import com.alocode.repository.MesaRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MesaService {
    private final com.alocode.repository.ClienteRepository clienteRepository;
    private final MesaRepository mesaRepository;
    
    public List<Mesa> obtenerTodasLasMesas() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return mesaRepository.findAllByClienteIdOrderByIdAsc(clienteId);
    }

    public List<Mesa> buscarMesas(String q) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        if (q == null || q.trim().isEmpty()) {
            return mesaRepository.findAllByClienteIdOrderByIdAsc(clienteId);
        }
        return mesaRepository.buscarPorNumeroOEstado(clienteId, q.trim());
    }
    
    public List<Mesa> obtenerMesasDisponibles() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return mesaRepository.findMesasDisponibles(clienteId);
    }
    
    public Mesa guardarMesa(Mesa mesa) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        if (mesa.getCliente() == null || !mesa.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("La mesa debe pertenecer al cliente actual");
        }
        Optional<Mesa> existente = mesaRepository.findByClienteIdAndNumero(clienteId, mesa.getNumero());
        if (existente.isPresent() && (mesa.getId() == null || !existente.get().getId().equals(mesa.getId()))) {
            throw new IllegalArgumentException("Ya existe una mesa con el número " + mesa.getNumero());
        }
        return mesaRepository.save(mesa);

    }

    public com.alocode.model.Cliente obtenerClientePorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }
    
    public void eliminarMesa(Long id) {
        mesaRepository.deleteById(id);
    }
    

    public Optional<Mesa> obtenerMesaPorId(Long id) {
        return mesaRepository.findById(id);
    }

        public void desactivarMesa(Long id) {
        Optional<Mesa> mesaOpt = mesaRepository.findById(id);
        if (mesaOpt.isPresent()) {
            Mesa mesa = mesaOpt.get();
            mesa.setEstado(com.alocode.model.enums.EstadoMesa.INACTIVA);
            mesaRepository.save(mesa);
        }
    }

    public void activarMesa(Long id) {
        Optional<Mesa> mesaOpt = mesaRepository.findById(id);
        if (mesaOpt.isPresent()) {
            Mesa mesa = mesaOpt.get();
            mesa.setEstado(com.alocode.model.enums.EstadoMesa.DISPONIBLE);
            mesaRepository.save(mesa);
        }
    }

    // Nuevos métodos para contar mesas activas e inactivas
    public long contarMesasActivas() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return mesaRepository.countByClienteIdAndActiva(clienteId);
    }

    public long contarMesasInactivas() {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        return mesaRepository.countByClienteIdAndInactiva(clienteId);
    }
    
}