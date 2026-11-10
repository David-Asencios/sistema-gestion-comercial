package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.OportunidadComercial;
import com.gestioncomercial.backend.entity.CanalOrigen;
import com.gestioncomercial.backend.entity.EtapaOportunidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OportunidadComercialRepository extends JpaRepository<OportunidadComercial, Long> {

    @Query("SELECT o FROM OportunidadComercial o JOIN FETCH o.cliente")
    List<OportunidadComercial> listarConCliente();

    @Query("SELECT o FROM OportunidadComercial o JOIN FETCH o.cliente WHERE o.id = :id")
    Optional<OportunidadComercial> buscarPorIdConCliente(@Param("id") Long id);

    @Query(value = """
            SELECT o FROM OportunidadComercial o JOIN FETCH o.cliente
            WHERE (:patron IS NULL OR LOWER(o.titulo) LIKE :patron ESCAPE '!')
              AND (:clienteId IS NULL OR o.cliente.id = :clienteId)
              AND (:etapa IS NULL OR o.etapa = :etapa)
              AND (:canal IS NULL OR o.canalOrigen = :canal)
            """, countQuery = """
            SELECT COUNT(o) FROM OportunidadComercial o
            WHERE (:patron IS NULL OR LOWER(o.titulo) LIKE :patron ESCAPE '!')
              AND (:clienteId IS NULL OR o.cliente.id = :clienteId)
              AND (:etapa IS NULL OR o.etapa = :etapa)
              AND (:canal IS NULL OR o.canalOrigen = :canal)
            """)
    Page<OportunidadComercial> buscar(
            @Param("patron") String patron, @Param("clienteId") Long clienteId,
            @Param("etapa") EtapaOportunidad etapa, @Param("canal") CanalOrigen canal, Pageable pagina);


}
