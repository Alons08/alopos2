package com.alocode.repository;

import com.alocode.model.ClienteConfiguracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteConfiguracionRepository extends JpaRepository<ClienteConfiguracion, Long> {
    ClienteConfiguracion findByClienteId(Long clienteId);
}
