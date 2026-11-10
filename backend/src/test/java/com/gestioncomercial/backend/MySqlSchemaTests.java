package com.gestioncomercial.backend;

import com.gestioncomercial.backend.dto.CotizacionMapper;
import com.gestioncomercial.backend.repository.CotizacionRepository;
import com.gestioncomercial.backend.repository.DetalleCotizacionRepository;
import com.gestioncomercial.backend.repository.SecuenciaCotizacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** Comprobación opcional de solo lectura. Nunca crea ni actualiza tablas. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@EnabledIfEnvironmentVariable(named = "MYSQL_VERIFY", matches = "true")
class MySqlSchemaTests {
    @Autowired CotizacionRepository cotizaciones;
    @Autowired DetalleCotizacionRepository detalles;
    @Autowired SecuenciaCotizacionRepository secuencia;

    @Test
    void validaEsquemaYLecturaDeDtos() {
        cotizaciones.listarConOportunidad().forEach(CotizacionMapper::toResponse);
        detalles.listarConRelaciones().forEach(CotizacionMapper::toResponse);
        long mayorNumero = cotizaciones.listarConOportunidad().stream()
                .map(c -> c.getCodigo()).filter(c -> c.matches("COT-[0-9]+"))
                .mapToLong(c -> Long.parseLong(c.substring(4))).max().orElse(0);
        assertThat(secuencia.findById(1L).orElseThrow().getUltimoNumero()).isGreaterThanOrEqualTo(mayorNumero);
    }
}
