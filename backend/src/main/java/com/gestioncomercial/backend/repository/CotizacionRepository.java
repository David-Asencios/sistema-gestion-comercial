package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotizacionRepository extends JpaRepository<Cotizacion,Long> {
}
