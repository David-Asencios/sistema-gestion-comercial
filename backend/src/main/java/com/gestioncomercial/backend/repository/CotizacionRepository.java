package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.entity.EstadoCotizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface CotizacionRepository extends JpaRepository<Cotizacion,Long> {

    @Query("""
            SELECT c FROM Cotizacion c
            JOIN FETCH c.oportunidad o
            JOIN FETCH o.cliente
            """)
    List<Cotizacion> listarConOportunidad();

    @Query(value = """
            SELECT c FROM Cotizacion c
            JOIN FETCH c.oportunidad o JOIN FETCH o.cliente
            WHERE (:patron IS NULL OR LOWER(c.codigo) LIKE :patron ESCAPE '!')
              AND (:clienteId IS NULL OR o.cliente.id = :clienteId)
              AND (:oportunidadId IS NULL OR o.id = :oportunidadId)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:desde IS NULL OR c.fechaEmision >= :desde)
              AND (:hasta IS NULL OR c.fechaEmision <= :hasta)
            """, countQuery = """
            SELECT COUNT(c) FROM Cotizacion c JOIN c.oportunidad o
            WHERE (:patron IS NULL OR LOWER(c.codigo) LIKE :patron ESCAPE '!')
              AND (:clienteId IS NULL OR o.cliente.id = :clienteId)
              AND (:oportunidadId IS NULL OR o.id = :oportunidadId)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:desde IS NULL OR c.fechaEmision >= :desde)
              AND (:hasta IS NULL OR c.fechaEmision <= :hasta)
            """)
    Page<Cotizacion> buscar(
            @Param("patron") String patron, @Param("clienteId") Long clienteId,
            @Param("oportunidadId") Long oportunidadId, @Param("estado") EstadoCotizacion estado,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, Pageable pagina);

    @Query("""
            SELECT c FROM Cotizacion c
            JOIN FETCH c.oportunidad o
            JOIN FETCH o.cliente
            WHERE c.id = :id
            """)
    Optional<Cotizacion> buscarPorIdConOportunidad(
            @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cotizacion c WHERE c.id = :id")
    Optional<Cotizacion> buscarPorIdParaActualizar(@Param("id") Long id);

    Optional<Cotizacion> findByCodigo(String codigo);

    @Query("""
        SELECT c FROM Cotizacion c
        JOIN FETCH c.oportunidad o
        JOIN FETCH o.cliente
        WHERE c.codigo = :codigo
        """)
    Optional<Cotizacion> buscarPorCodigoConOportunidad(
            @Param("codigo") String codigo
    );

}
