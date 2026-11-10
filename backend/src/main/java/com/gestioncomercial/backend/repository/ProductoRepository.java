package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Query("""
            SELECT c FROM Producto c
            WHERE (:patron IS NULL OR LOWER(c.nombre) LIKE :patron ESCAPE '!')
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Producto> buscar(@Param("patron") String patron, @Param("activo") Boolean activo, Pageable pagina);
}
