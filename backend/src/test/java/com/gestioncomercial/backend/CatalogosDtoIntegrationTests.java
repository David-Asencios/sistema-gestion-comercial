package com.gestioncomercial.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Peticiones con transacciones independientes sobre una H2 exclusiva. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogos_dto_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CatalogosDtoIntegrationTests {
    @Autowired MockMvc mvc;

    private ResultActions crear(String ruta, String cuerpo) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private long crearId(String ruta, String cuerpo) throws Exception {
        String respuesta = crear(ruta, cuerpo).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "productos"})
    void crudProtegeIdYConservaFormatoYValoresPredeterminados(String catalogo) throws Exception {
        String ruta = "/api/" + catalogo;
        long original = crearId(ruta, "{\"nombre\":\"Original\",\"precioReferencial\":100}");
        long nuevo = crearId(ruta, """
                {"id":%d,"nombre":"Nuevo","descripcion":"Descripción","precioReferencial":180,"activo":false}
                """.formatted(original));
        assertThat(nuevo).isNotEqualTo(original);
        mvc.perform(get(ruta + "/{id}", original)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Original"))
                .andExpect(jsonPath("$.activo").value(true));
        mvc.perform(put(ruta + "/{id}", nuevo).contentType(MediaType.APPLICATION_JSON).content("""
                {"id":%d,"nombre":"Actualizado","descripcion":"Editado","precioReferencial":250.50}
                """.formatted(original))).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(nuevo)).andExpect(jsonPath("$.activo").value(true));
        mvc.perform(get(ruta + "/{id}", nuevo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Actualizado"))
                .andExpect(jsonPath("$.descripcion").value("Editado"))
                .andExpect(jsonPath("$.precioReferencial").value(250.50));
        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + nuevo + ")]").isNotEmpty());
        mvc.perform(delete(ruta + "/{id}", nuevo)).andExpect(status().isNoContent());
        mvc.perform(get(ruta + "/{id}", nuevo)).andExpect(status().isNotFound());
        mvc.perform(put(ruta + "/{id}", nuevo).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"No existe\",\"precioReferencial\":10}")).andExpect(status().isNotFound());
        mvc.perform(delete(ruta + "/{id}", nuevo)).andExpect(status().isNotFound());
        mvc.perform(get(ruta + "/{id}", original)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Original"))
                .andExpect(jsonPath("$.precioReferencial").value(100));
    }

    static Stream<Arguments> entradasInvalidas() {
        return Stream.of("servicios", "productos").flatMap(catalogo -> Stream.of(
                Arguments.of(catalogo, "nombre", "{\"precioReferencial\":10}"),
                Arguments.of(catalogo, "nombre", "{\"nombre\":null,\"precioReferencial\":10}"),
                Arguments.of(catalogo, "nombre", "{\"nombre\":\" \",\"precioReferencial\":10}"),
                Arguments.of(catalogo, "nombre", "{\"nombre\":\"" + "a".repeat(151) + "\",\"precioReferencial\":10}"),
                Arguments.of(catalogo, "precioReferencial", "{\"nombre\":\"Inválido\"}"),
                Arguments.of(catalogo, "precioReferencial", "{\"nombre\":\"Inválido\",\"precioReferencial\":null}"),
                Arguments.of(catalogo, "precioReferencial", "{\"nombre\":\"Inválido\",\"precioReferencial\":-0.01}"),
                Arguments.of(catalogo, "precioReferencial", "{\"nombre\":\"Inválido\",\"precioReferencial\":100000000}"),
                Arguments.of(catalogo, "precioReferencial", "{\"nombre\":\"Inválido\",\"precioReferencial\":10.001}"),
                Arguments.of(catalogo, "activo", "{\"nombre\":\"Inválido\",\"precioReferencial\":10,\"activo\":null}")
        ));
    }

    @ParameterizedTest
    @MethodSource("entradasInvalidas")
    void entradasInvalidasDevuelven400SinCrearNiModificar(String catalogo, String campo, String cuerpo) throws Exception {
        String ruta = "/api/" + catalogo;
        long id = crearId(ruta, "{\"nombre\":\"Conservar\",\"precioReferencial\":25}");
        int cantidad = JsonPath.<java.util.List<?>>read(mvc.perform(get(ruta)).andReturn()
                .getResponse().getContentAsString(), "$").size();
        crear(ruta, cuerpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$." + campo).exists());
        mvc.perform(put(ruta + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$." + campo).exists());
        mvc.perform(get(ruta + "/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Conservar"))
                .andExpect(jsonPath("$.precioReferencial").value(25)).andExpect(jsonPath("$.activo").value(true));
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(cantidad));
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "productos"})
    void admitePrecioCeroYMaximoDeLaColumna(String catalogo) throws Exception {
        String ruta = "/api/" + catalogo;
        long id = crearId(ruta, "{\"nombre\":\"" + "a".repeat(150) + "\",\"precioReferencial\":0}");
        mvc.perform(put(ruta + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Máximo\",\"precioReferencial\":99999999.99}"))
                .andExpect(status().isOk());
        mvc.perform(get(ruta + "/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.precioReferencial").value(99999999.99));
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "productos"})
    void editarCatalogoNoAlteraFotografiaComercialDeCotizacionAceptada(String catalogo) throws Exception {
        String ruta = "/api/" + catalogo;
        String concepto = catalogo.equals("servicios") ? "servicio" : "producto";
        long catalogoId = crearId(ruta, "{\"nombre\":\"Catálogo original\",\"precioReferencial\":180}");
        long cliente = crearId("/api/clientes", "{\"razonSocial\":\"Cliente\",\"nombreContacto\":\"Contacto\"}");
        long oportunidad = crearId("/api/oportunidades", """
                {"titulo":"Oportunidad","canalOrigen":"OTRO","cliente":{"id":%d}}
                """.formatted(cliente));
        long cotizacion = crearId("/api/cotizaciones", "{\"oportunidad\":{\"id\":" + oportunidad + "}}");
        long detalle = crearId("/api/detalles-cotizacion", """
                {"cotizacion":{"id":%d},"%s":{"id":%d},"descripcion":"Concepto histórico",
                 "cantidad":7,"precioUnitario":180,"tipoDescuento":"MONTO","valorDescuento":30}
                """.formatted(cotizacion, concepto, catalogoId));
        for (String estado : new String[]{"ENVIADA", "ACEPTADA"}) {
            mvc.perform(patch("/api/cotizaciones/{id}/estado", cotizacion)
                    .contentType(MediaType.APPLICATION_JSON).content("\"" + estado + "\""))
                    .andExpect(status().isOk());
        }
        mvc.perform(put(ruta + "/{id}", catalogoId).contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Catálogo actualizado","descripcion":"Nueva descripción","precioReferencial":999,"activo":false}
                """)).andExpect(status().isOk());
        mvc.perform(get("/api/detalles-cotizacion/{id}", detalle)).andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Concepto histórico"))
                .andExpect(jsonPath("$.cantidad").value(7)).andExpect(jsonPath("$.precioUnitario").value(180))
                .andExpect(jsonPath("$.tipoDescuento").value("MONTO")).andExpect(jsonPath("$.valorDescuento").value(30))
                .andExpect(jsonPath("$.descuentoUnitario").value(30)).andExpect(jsonPath("$.descuentoTotal").value(210))
                .andExpect(jsonPath("$.subtotal").value(1050))
                .andExpect(jsonPath("$." + concepto + ".nombre").value("Catálogo actualizado"))
                .andExpect(jsonPath("$." + concepto + ".precioReferencial").value(999));
        mvc.perform(delete(ruta + "/{id}", catalogoId)).andExpect(status().isConflict());
        mvc.perform(get("/api/cotizaciones/{id}", cotizacion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA")).andExpect(jsonPath("$.subtotal").value(1050))
                .andExpect(jsonPath("$.igv").value(189)).andExpect(jsonPath("$.total").value(1239));
    }
}
