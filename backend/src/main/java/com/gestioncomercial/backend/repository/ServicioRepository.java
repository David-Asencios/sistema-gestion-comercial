package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    @Query("""
            SELECT c FROM Servicio c
            WHERE (:patron IS NULL OR LOWER(c.nombre) LIKE :patron ESCAPE '!')
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Servicio> buscar(@Param("patron") String patron, @Param("activo") Boolean activo, Pageable pagina);
}
