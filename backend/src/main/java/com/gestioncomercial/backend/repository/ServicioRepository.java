package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
}
