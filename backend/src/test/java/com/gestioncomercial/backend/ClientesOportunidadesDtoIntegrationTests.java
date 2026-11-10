package com.gestioncomercial.backend;

import com.gestioncomercial.backend.entity.Cliente;
import com.gestioncomercial.backend.repository.ClienteRepository;
import com.gestioncomercial.backend.repository.OportunidadComercialRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Sin transacción de prueba: verifica también el mapeo con open-in-view desactivado. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:clientes_oportunidades_dto_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ClientesOportunidadesDtoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ClienteRepository clientes;
    @Autowired OportunidadComercialRepository oportunidades;
    private Cliente existente;

    @BeforeEach
    void preparar() {
        oportunidades.deleteAll();
        clientes.deleteAll();
        existente = new Cliente();
        existente.setRazonSocial("Cliente original");
        existente.setNombreContacto("Contacto original");
        existente = clientes.save(existente);
    }

    @Test
    void crearClienteIgnoraIdAjenoYActualizarUsaIdDeRuta() throws Exception {
        String cuerpo = """
                {"id":%d,"razonSocial":"Cliente nuevo","nombreContacto":"Contacto",
                 "activo":false,"telefono":"999999999","correo":"contacto@example.com","direccion":"Lima"}
                """.formatted(existente.getId());
        String respuesta = mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.activo").value(false))
                .andReturn().getResponse().getContentAsString();
        long nuevoId = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        assertThat(nuevoId).isNotEqualTo(existente.getId());
        assertThat(clientes.findById(existente.getId()).orElseThrow().getRazonSocial()).isEqualTo("Cliente original");
        mvc.perform(put("/api/clientes/{id}", nuevoId).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("Cliente nuevo", "Cliente actualizado")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(nuevoId))
                .andExpect(jsonPath("$.razonSocial").value("Cliente actualizado"));
        mvc.perform(get("/api/clientes/{id}", nuevoId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correo").value("contacto@example.com"))
                .andExpect(jsonPath("$.telefono").value("999999999")).andExpect(jsonPath("$.direccion").value("Lima"));
        mvc.perform(get("/api/clientes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void oportunidadProtegeIdYFechaCargaClienteYPermiteActualizarYConsultarSinSesionAbierta() throws Exception {
        String cuerpo = """
                {"id":99999999,"fechaCreacion":"2000-01-01T00:00:00","titulo":"Oportunidad DTO",
                 "descripcion":"Descripción","canalOrigen":"WHATSAPP","observaciones":"Observación",
                 "cliente":{"id":%d,"razonSocial":"Nombre inyectado","activo":false}}
                """.formatted(existente.getId());
        String respuesta = mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.etapa").value("NUEVA"))
                .andExpect(jsonPath("$.cliente.razonSocial").value("Cliente original"))
                .andExpect(jsonPath("$.cliente.activo").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        assertThat(id).isNotEqualTo(99999999L);
        // Comparar con la fecha persistida: la base redondea los nanos a microsegundos.
        String consulta = mvc.perform(get("/api/oportunidades/{id}", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String fecha = JsonPath.read(consulta, "$.fechaCreacion");
        assertThat(fecha).doesNotStartWith("2000-");
        mvc.perform(put("/api/oportunidades/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.replace("Oportunidad DTO", "Oportunidad actualizada")
                                .replace("\"descripcion\":\"Descripción\"", "\"etapa\":\"EN_NEGOCIACION\",\"descripcion\":\"Descripción\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.fechaCreacion").value(fecha))
                .andExpect(jsonPath("$.etapa").value("EN_NEGOCIACION"));
        mvc.perform(get("/api/oportunidades/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.id").value(existente.getId()))
                .andExpect(jsonPath("$.titulo").value("Oportunidad actualizada"))
                .andExpect(jsonPath("$.descripcion").value("Descripción"))
                .andExpect(jsonPath("$.observaciones").value("Observación"));
        mvc.perform(get("/api/oportunidades")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cliente.razonSocial").value("Cliente original"));
        assertThat(clientes.findById(existente.getId()).orElseThrow().getActivo()).isTrue();
        assertThat(oportunidades.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"razonSocial\":\"\"", "\"nombreContacto\":\" \"", "\"correo\":\"incorrecto\"",
            "\"telefono\":\"1234567890\"", "\"ruc\":\"123456789012\"", "\"activo\":null"
    })
    void clienteInvalidoDevuelveErroresPorCampoYNoPersiste(String campo) throws Exception {
        String cuerpo = "{\"razonSocial\":\"Válido\",\"nombreContacto\":\"Contacto\"," + campo + "}";
        // Sustituir el campo, evitando claves JSON duplicadas.
        if (campo.startsWith("\"razonSocial\"")) cuerpo = "{\"nombreContacto\":\"Contacto\"," + campo + "}";
        if (campo.startsWith("\"nombreContacto\"")) cuerpo = "{\"razonSocial\":\"Válido\"," + campo + "}";
        String nombre = campo.substring(1, campo.indexOf('"', 1));
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$." + nombre).exists());
        mvc.perform(put("/api/clientes/{id}", existente.getId()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
        assertThat(clientes.count()).isEqualTo(1);
        assertThat(clientes.findById(existente.getId()).orElseThrow().getRazonSocial()).isEqualTo("Cliente original");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"titulo\":\" \"", "\"canalOrigen\":null", "\"etapa\":null"})
    void oportunidadInvalidaNoPersiste(String campo) throws Exception {
        String cuerpo = "{\"cliente\":{\"id\":" + existente.getId() + "}," + campo
                + (campo.startsWith("\"titulo\"") ? ",\"canalOrigen\":\"WHATSAPP\"}" :
                campo.startsWith("\"canalOrigen\"") ? ",\"titulo\":\"Válido\"}" :
                ",\"titulo\":\"Válido\",\"canalOrigen\":\"WHATSAPP\"}");
        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
        assertThat(oportunidades.count()).isZero();
    }

    @Test
    void permiteEliminarOportunidadYClienteSinRelaciones() throws Exception {
        String respuesta = mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Eliminar","canalOrigen":"OTRO","cliente":{"id":%d}}
                """.formatted(existente.getId()))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(respuesta, "$.id")).longValue();
        mvc.perform(delete("/api/oportunidades/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/oportunidades/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/clientes/{id}", existente.getId())).andExpect(status().isNoContent());
        mvc.perform(get("/api/clientes/{id}", existente.getId())).andExpect(status().isNotFound());
    }
}
