package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente,Long> {

    boolean existsByRuc(String ruc);

    @Query("""
            SELECT c FROM Cliente c
            WHERE (:activo IS NULL OR c.activo = :activo)
              AND (:patron IS NULL OR LOWER(c.razonSocial) LIKE :patron ESCAPE '!'
                   OR LOWER(c.ruc) LIKE :patron ESCAPE '!'
                   OR LOWER(c.nombreContacto) LIKE :patron ESCAPE '!')
            """)
    Page<Cliente> buscar(@Param("patron") String patron, @Param("activo") Boolean activo, Pageable pagina);

}
