package com.gestioncomercial.backend;

import com.gestioncomercial.backend.repository.ProductoRepository;
import com.gestioncomercial.backend.repository.ServicioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:busquedas_catalogos_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BusquedasCatalogosIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ServicioRepository servicios;
    @Autowired ProductoRepository productos;

    @BeforeEach
    void limpiarBaseAislada() {
        servicios.deleteAll();
        productos.deleteAll();
    }

    private List<Long> preparar(String recurso) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String nombre = i == 2 ? "Venta 100%_!" : "Alquiler equipo";
            String cuerpo = """
                    {"nombre":"%s","descripcion":"alquiler solo descripción","precioReferencial":%d,"activo":%s}
                    """.formatted(nombre, i == 2 ? 0 : 180, i == 1 ? "false" : "true");
            String respuesta = mvc.perform(post("/api/" + recurso).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            ids.add(((Number) JsonPath.read(respuesta, "$.id")).longValue());
        }
        return ids;
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "productos"})
    void predeterminadosYListadoAnteriorConservanElContrato(String recurso) throws Exception {
        preparar(recurso);
        mvc.perform(get("/api/" + recurso + "/buscar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(4))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Alquiler equipo"))
                .andExpect(jsonPath("$.contenido[0].descripcion").value("alquiler solo descripción"))
                .andExpect(jsonPath("$.contenido[0].precioReferencial").value(180))
                .andExpect(jsonPath("$.contenido[0].activo").value(true))
                .andExpect(jsonPath("$.pagina").value(0)).andExpect(jsonPath("$.tamano").value(20))
                .andExpect(jsonPath("$.totalElementos").value(4)).andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.primera").value(true)).andExpect(jsonPath("$.ultima").value(true));
        mvc.perform(get("/api/" + recurso)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "productos"})
    void buscaSoloEnNombreYCombinaActivoSinConfundirFalseConAusencia(String recurso) throws Exception {
        List<Long> ids = preparar(recurso);
        String ruta = "/api/" + recurso + "/buscar";
        mvc.perform(get(ruta).param("texto", "  ALQUILER  ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));
        mvc.perform(get(ruta).param("texto", "equipo").param("activo", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get(ruta).param("texto", "equipo").param("activo", "false")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(ids.get(1)))
                .andExpect(jsonPath("$.contenido[0].activo").value(false));
        mvc.perform(get(ruta).param("activo", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));
        mvc.perform(get(ruta).param("texto", "solo descripción")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get(ruta).param("texto", "   ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(4));
    }

    static Stream<Arguments> caracteresLiterales() {
        return Stream.of("servicios", "productos").flatMap(recurso ->
                Stream.of("%", "_", "!").map(caracter -> Arguments.of(recurso, caracter)));
    }

    @ParameterizedTest
    @MethodSource("caracteresLiterales")
    void buscaCaracteresEspecialesLiteralmente(String recurso, String texto) throws Exception {
        preparar(recurso);
        mvc.perform(get("/api/" + recurso + "/buscar").param("texto", texto))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Venta 100%_!"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "productos"})
    void precioConEmpatesSePaginaSinDuplicarNiPerderFilas(String recurso) throws Exception {
        List<Long> ids = preparar(recurso);
        List<Long> encontrados = new ArrayList<>();
        for (int pagina = 0; pagina < 2; pagina++) {
            String respuesta = mvc.perform(get("/api/" + recurso + "/buscar").param("texto", "alquiler")
                            .param("orden", "precioReferencial").param("pagina", "" + pagina).param("tamano", "2"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(3))
                    .andExpect(jsonPath("$.totalPaginas").value(2))
                    .andExpect(jsonPath("$.primera").value(pagina == 0))
                    .andExpect(jsonPath("$.ultima").value(pagina == 1))
                    .andReturn().getResponse().getContentAsString();
            encontrados.addAll(JsonPath.<List<Number>>read(respuesta, "$.contenido[*].id")
                    .stream().map(Number::longValue).toList());
        }
        assertThat(encontrados).containsExactly(ids.get(0), ids.get(1), ids.get(3)).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "productos"})
    void ordenaPorNombrePrecioEIdEnAmbasDirecciones(String recurso) throws Exception {
        List<Long> ids = preparar(recurso);
        String ruta = "/api/" + recurso + "/buscar";
        for (String orden : List.of("id", "nombre", "precioReferencial")) {
            mvc.perform(get(ruta).param("orden", orden).param("direccion", "asc"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(4));
            mvc.perform(get(ruta).param("orden", orden).param("direccion", "DESC"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(4));
        }
        mvc.perform(get(ruta).param("orden", "precioReferencial").param("tamano", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].id").value(ids.get(2)));
        mvc.perform(get(ruta).param("orden", "precioReferencial").param("direccion", "desc").param("tamano", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].id").value(ids.get(3)));
        mvc.perform(get(ruta).param("orden", "nombre").param("direccion", "desc").param("tamano", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].nombre").value("Venta 100%_!"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"servicios", "productos"})
    void sinResultadosYPaginaFueraDelListadoSon200(String recurso) throws Exception {
        preparar(recurso);
        String ruta = "/api/" + recurso + "/buscar";
        mvc.perform(get(ruta).param("texto", "inexistente")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty()).andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0))
                .andExpect(jsonPath("$.primera").value(true)).andExpect(jsonPath("$.ultima").value(true));
        mvc.perform(get(ruta).param("pagina", "99").param("tamano", "100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty()).andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.totalPaginas").value(1)).andExpect(jsonPath("$.pagina").value(99));
    }

    static Stream<Arguments> parametrosInvalidos() {
        return Stream.of("servicios", "productos").flatMap(recurso -> Stream.of(
                Arguments.of(recurso, "pagina", "-1"), Arguments.of(recurso, "pagina", "abc"),
                Arguments.of(recurso, "pagina", "2147483648"), Arguments.of(recurso, "pagina", "2147483647"),
                Arguments.of(recurso, "tamano", "0"), Arguments.of(recurso, "tamano", "101"),
                Arguments.of(recurso, "tamano", "abc"), Arguments.of(recurso, "orden", "descripcion"),
                Arguments.of(recurso, "direccion", "lateral"), Arguments.of(recurso, "activo", "invalido"),
                Arguments.of(recurso, "texto", "a".repeat(201))
        ));
    }

    @ParameterizedTest
    @MethodSource("parametrosInvalidos")
    void parametrosInvalidosDevuelven400(String recurso, String campo, String valor) throws Exception {
        mvc.perform(get("/api/" + recurso + "/buscar").param(campo, valor))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
    }
}
