package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
