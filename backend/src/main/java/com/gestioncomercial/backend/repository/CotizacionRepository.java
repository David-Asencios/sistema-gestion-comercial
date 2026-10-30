package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion,Long> {

    @Query("""
            SELECT c FROM Cotizacion c
            JOIN FETCH c.oportunidad o
            JOIN FETCH o.cliente
            """)
    List<Cotizacion> listarConOportunidad();

    @Query("""
            SELECT c FROM Cotizacion c
            JOIN FETCH c.oportunidad o
            JOIN FETCH o.cliente
            WHERE c.id = :id
            """)
    Optional<Cotizacion> buscarPorIdConOportunidad(
            @Param("id") Long id);

    @Query("""
        SELECT MAX(c.codigo)
        FROM Cotizacion c
        WHERE c.codigo LIKE :prefijo
        """)
    String obtenerUltimoCodigo(@Param("prefijo") String prefijo);

}
