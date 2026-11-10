package com.gestioncomercial.backend.repository;

import com.gestioncomercial.backend.entity.SecuenciaCotizacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface SecuenciaCotizacionRepository extends JpaRepository<SecuenciaCotizacion, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaCotizacion s WHERE s.id = 1")
    Optional<SecuenciaCotizacion> obtenerParaActualizar();
}
