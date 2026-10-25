package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente,Long> {
}
