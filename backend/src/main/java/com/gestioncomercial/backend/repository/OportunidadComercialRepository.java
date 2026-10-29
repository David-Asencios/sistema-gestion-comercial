package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.OportunidadComercial;
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


}
