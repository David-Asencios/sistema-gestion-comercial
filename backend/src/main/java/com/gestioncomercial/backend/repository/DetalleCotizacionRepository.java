
package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.DetalleCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DetalleCotizacionRepository
        extends JpaRepository<DetalleCotizacion, Long> {

    @Query("""
            SELECT COALESCE(SUM(d.subtotal), 0)
            FROM DetalleCotizacion d
            WHERE d.cotizacion.id = :cotizacionId
            """)
    BigDecimal sumarSubtotalPorCotizacion(
            @Param("cotizacionId") Long cotizacionId);

    @Query("""
        SELECT d FROM DetalleCotizacion d
        JOIN FETCH d.cotizacion c
        JOIN FETCH c.oportunidad o
        JOIN FETCH o.cliente
        LEFT JOIN FETCH d.servicio
        LEFT JOIN FETCH d.producto
        """)
    List<DetalleCotizacion> listarConRelaciones();

    @Query("""
        SELECT d FROM DetalleCotizacion d
        JOIN FETCH d.cotizacion c
        JOIN FETCH c.oportunidad o
        JOIN FETCH o.cliente
        LEFT JOIN FETCH d.servicio
        LEFT JOIN FETCH d.producto
        WHERE d.id = :id
        """)
    Optional<DetalleCotizacion> buscarPorIdConRelaciones(
            @Param("id") Long id);


    boolean existsByCotizacionId(Long cotizacionId);


}
