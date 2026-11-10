package com.gestioncomercial.backend;

import com.gestioncomercial.backend.entity.*;
import com.gestioncomercial.backend.repository.ClienteRepository;
import com.gestioncomercial.backend.repository.OportunidadComercialRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:busquedas_comerciales_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BusquedasComercialesIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ClienteRepository clientes;
    @Autowired OportunidadComercialRepository oportunidades;
    private Cliente alfa;
    private Cliente beta;
    private List<Long> alquileres;
    private List<Long> combinadas;

    @BeforeEach
    void preparar() {
        oportunidades.deleteAll();
        clientes.deleteAll();
        alfa = cliente("Alfa equipos", "20111111111", "Lucia", true);
        beta = cliente("Beta", "20222222222", "ALFA contacto", false);
        Cliente gamma = cliente("Gamma 100%_!", null, "Marco", true);
        cliente("Delta", "20333333333", "Pedro", true);
        long a = oportunidad("Alquiler", alfa, EtapaOportunidad.NUEVA, CanalOrigen.WHATSAPP, 1);
        long b = oportunidad("Alquiler", alfa, EtapaOportunidad.EN_NEGOCIACION, CanalOrigen.WHATSAPP, 2);
        long c = oportunidad("Alquiler", beta, EtapaOportunidad.NUEVA, CanalOrigen.LLAMADA, 3);
        oportunidad("Venta 100%_!", gamma, EtapaOportunidad.GANADA, CanalOrigen.WHATSAPP, 4);
        oportunidad("Servicio", alfa, EtapaOportunidad.NUEVA, CanalOrigen.WHATSAPP, 5);
        long f = oportunidad("Alquiler", alfa, EtapaOportunidad.NUEVA, CanalOrigen.WHATSAPP, 6);
        alquileres = List.of(a, b, c, f);
        combinadas = List.of(a, f);
    }

    private Cliente cliente(String nombre, String ruc, String contacto, boolean activo) {
        Cliente cliente = new Cliente();
        cliente.setRazonSocial(nombre);
        cliente.setRuc(ruc);
        cliente.setNombreContacto(contacto);
        cliente.setActivo(activo);
        return clientes.save(cliente);
    }

    private long oportunidad(String titulo, Cliente cliente, EtapaOportunidad etapa, CanalOrigen canal, int dia) {
        OportunidadComercial oportunidad = new OportunidadComercial();
        oportunidad.setTitulo(titulo);
        oportunidad.setCliente(cliente);
        oportunidad.setEtapa(etapa);
        oportunidad.setCanalOrigen(canal);
        oportunidad.setFechaCreacion(LocalDateTime.of(2026, 1, dia, 12, 0));
        return oportunidades.save(oportunidad).getId();
    }

    private List<Long> ids(String json) {
        return JsonPath.<List<Number>>read(json, "$.contenido[*].id").stream().map(Number::longValue).toList();
    }

    @ParameterizedTest
    @CsvSource({"clientes,4", "oportunidades,6"})
    void valoresPredeterminadosYCompatibilidadDelListado(String recurso, int cantidad) throws Exception {
        mvc.perform(get("/api/" + recurso + "/buscar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(cantidad))
                .andExpect(jsonPath("$.pagina").value(0)).andExpect(jsonPath("$.tamano").value(20))
                .andExpect(jsonPath("$.totalElementos").value(cantidad)).andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.primera").value(true)).andExpect(jsonPath("$.ultima").value(true));
        mvc.perform(get("/api/" + recurso)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(cantidad));
    }

    @ParameterizedTest
    @CsvSource({"alfa,2", "201111,1", "LUCIA,1", "inexistente,0", "'   ',4"})
    void clientesBuscaPorRazonRucOContacto(String texto, int cantidad) throws Exception {
        mvc.perform(get("/api/clientes/buscar").param("texto", texto))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(cantidad));
    }

    @Test
    void clientesCombinaTextoYActivoYNoConfundeFalseConAusencia() throws Exception {
        mvc.perform(get("/api/clientes/buscar").param("texto", "  ALFA  ").param("activo", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(beta.getId()));
        mvc.perform(get("/api/clientes/buscar").param("activo", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!"})
    void caracteresLikeSeBuscanLiteralmenteEnAmbosModulos(String texto) throws Exception {
        for (String recurso : List.of("clientes", "oportunidades")) {
            mvc.perform(get("/api/" + recurso + "/buscar").param("texto", texto))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        }
    }

    @Test
    void oportunidadesCombinaTodosLosFiltrosYPaginaConClienteCargado() throws Exception {
        for (int pagina = 0; pagina < 2; pagina++) {
            mvc.perform(get("/api/oportunidades/buscar").param("texto", "  aLQUILer  ")
                            .param("clienteId", alfa.getId().toString()).param("etapa", "NUEVA")
                            .param("canalOrigen", "WHATSAPP").param("tamano", "1").param("pagina", "" + pagina))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(1))
                    .andExpect(jsonPath("$.contenido[0].id").value(combinadas.get(pagina)))
                    .andExpect(jsonPath("$.contenido[0].cliente.razonSocial").value("Alfa equipos"))
                    .andExpect(jsonPath("$.totalElementos").value(2)).andExpect(jsonPath("$.totalPaginas").value(2))
                    .andExpect(jsonPath("$.primera").value(pagina == 0)).andExpect(jsonPath("$.ultima").value(pagina == 1));
        }
    }

    @ParameterizedTest
    @CsvSource({"etapa,GANADA,1", "canalOrigen,LLAMADA,1", "texto,servicio,1", "texto,inexistente,0"})
    void filtrosIndividualesDeOportunidades(String campo, String valor, int cantidad) throws Exception {
        mvc.perform(get("/api/oportunidades/buscar").param(campo, valor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(cantidad));
    }

    @Test
    void filtraPorClienteYDevuelveVacioParaClienteInexistente() throws Exception {
        mvc.perform(get("/api/oportunidades/buscar").param("clienteId", beta.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/oportunidades/buscar").param("clienteId", "999999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0)).andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void ordenEstableDesempataPorIdYNoDuplicaNiPierdeEntrePaginas() throws Exception {
        List<Long> encontrados = new ArrayList<>();
        for (int pagina = 0; pagina < 2; pagina++) {
            String respuesta = mvc.perform(get("/api/oportunidades/buscar").param("texto", "alquiler")
                            .param("orden", "titulo").param("pagina", "" + pagina).param("tamano", "2"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(4))
                    .andReturn().getResponse().getContentAsString();
            encontrados.addAll(ids(respuesta));
        }
        assertThat(encontrados).containsExactlyElementsOf(alquileres).doesNotHaveDuplicates();
        mvc.perform(get("/api/oportunidades/buscar").param("orden", "fechaCreacion").param("direccion", "DESC")
                        .param("tamano", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(alquileres.getLast()));
        mvc.perform(get("/api/clientes/buscar").param("orden", "razonSocial").param("direccion", "desc")
                        .param("tamano", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].razonSocial").value("Gamma 100%_!"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"clientes", "oportunidades"})
    void paginaFueraDelResultadoYTamanoMaximoSonValidos(String recurso) throws Exception {
        mvc.perform(get("/api/" + recurso + "/buscar").param("pagina", "99").param("tamano", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isEmpty())
                .andExpect(jsonPath("$.pagina").value(99)).andExpect(jsonPath("$.ultima").value(true));
        mvc.perform(get("/api/" + recurso + "/buscar").param("texto", "sin resultados"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0)).andExpect(jsonPath("$.totalPaginas").value(0))
                .andExpect(jsonPath("$.primera").value(true)).andExpect(jsonPath("$.ultima").value(true));
    }

    static Stream<Arguments> parametrosInvalidos() {
        return Stream.of("clientes", "oportunidades").flatMap(recurso -> Stream.of(
                Arguments.of(recurso, "pagina", "-1"), Arguments.of(recurso, "pagina", "abc"),
                Arguments.of(recurso, "pagina", "2147483648"), Arguments.of(recurso, "pagina", "2147483647"),
                Arguments.of(recurso, "tamano", "0"), Arguments.of(recurso, "tamano", "101"),
                Arguments.of(recurso, "tamano", "abc"), Arguments.of(recurso, "orden", "cliente.password"),
                Arguments.of(recurso, "direccion", "lateral"), Arguments.of(recurso, "texto", "a".repeat(201))
        ));
    }

    @ParameterizedTest
    @MethodSource("parametrosInvalidos")
    void parametrosInvalidosDevuelven400(String recurso, String parametro, String valor) throws Exception {
        mvc.perform(get("/api/" + recurso + "/buscar").param(parametro, valor))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
    }

    @ParameterizedTest
    @CsvSource({"clienteId,0", "clienteId,-1", "clienteId,abc", "etapa,INVALIDA", "canalOrigen,INVALIDO"})
    void filtrosInvalidosDeOportunidadesDevuelven400(String campo, String valor) throws Exception {
        mvc.perform(get("/api/oportunidades/buscar").param(campo, valor))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void activoInvalidoDevuelve400() throws Exception {
        mvc.perform(get("/api/clientes/buscar").param("activo", "invalido"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
    }
}
