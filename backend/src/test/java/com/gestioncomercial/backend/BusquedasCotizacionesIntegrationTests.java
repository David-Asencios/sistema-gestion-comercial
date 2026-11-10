package com.gestioncomercial.backend;

import com.gestioncomercial.backend.entity.*;
import com.gestioncomercial.backend.repository.ClienteRepository;
import com.gestioncomercial.backend.repository.CotizacionRepository;
import com.gestioncomercial.backend.repository.OportunidadComercialRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:busquedas_cotizaciones_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BusquedasCotizacionesIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ClienteRepository clientes;
    @Autowired OportunidadComercialRepository oportunidades;
    @Autowired CotizacionRepository cotizaciones;
    private Cliente alfa;
    private OportunidadComercial primera;
    private OportunidadComercial segunda;
    private List<Cotizacion> registros;

    @BeforeEach
    void preparar() {
        cotizaciones.deleteAll();
        oportunidades.deleteAll();
        clientes.deleteAll();
        alfa = cliente("Cliente Alfa");
        Cliente beta = cliente("Cliente Beta");
        primera = oportunidad(alfa, "Primera");
        segunda = oportunidad(beta, "Segunda");
        OportunidadComercial tercera = oportunidad(alfa, "Tercera");
        // Fotografía de estados históricos: no se simulan transiciones ni se usa MySQL.
        registros = List.of(
                cotizacion("COT-00001", primera, EstadoCotizacion.BORRADOR, 1, 100),
                cotizacion("COT-00002", primera, EstadoCotizacion.ENVIADA, 2, 100),
                cotizacion("COT-00003", segunda, EstadoCotizacion.ACEPTADA, 3, 300),
                cotizacion("COT-00004", tercera, EstadoCotizacion.RECHAZADA, 4, 400),
                cotizacion("COT-00005", primera, EstadoCotizacion.VENCIDA, 5, 500),
                cotizacion("COT-100000", primera, EstadoCotizacion.BORRADOR, 2, 100));
    }

    private Cliente cliente(String nombre) {
        Cliente c = new Cliente();
        c.setRazonSocial(nombre);
        c.setNombreContacto("Contacto");
        return clientes.save(c);
    }

    private OportunidadComercial oportunidad(Cliente cliente, String titulo) {
        OportunidadComercial o = new OportunidadComercial();
        o.setCliente(cliente);
        o.setTitulo(titulo);
        o.setCanalOrigen(CanalOrigen.WHATSAPP);
        return oportunidades.save(o);
    }

    private Cotizacion cotizacion(String codigo, OportunidadComercial oportunidad,
                                   EstadoCotizacion estado, int dia, int subtotal) {
        Cotizacion c = new Cotizacion();
        c.setCodigo(codigo);
        c.setOportunidad(oportunidad);
        c.setEstado(estado);
        c.setFechaEmision(LocalDate.of(2026, 1, dia));
        c.setFechaVencimiento(c.getFechaEmision().plusDays(15));
        c.setSubtotal(BigDecimal.valueOf(subtotal));
        c.setIgv(c.getSubtotal().multiply(new BigDecimal("0.18")));
        c.setTotal(c.getSubtotal().add(c.getIgv()));
        return cotizaciones.save(c);
    }

    @Test
    void predeterminadosYRelacionesDisponiblesSinTransaccionDePrueba() throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(6))
                .andExpect(jsonPath("$.contenido[0].oportunidad.id").value(primera.getId()))
                .andExpect(jsonPath("$.contenido[0].oportunidad.cliente.razonSocial").value("Cliente Alfa"))
                .andExpect(jsonPath("$.pagina").value(0)).andExpect(jsonPath("$.tamano").value(20))
                .andExpect(jsonPath("$.totalElementos").value(6)).andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.primera").value(true)).andExpect(jsonPath("$.ultima").value(true));
    }

    @ParameterizedTest
    @CsvSource({"'  cot-  ',6", "00003,1", "COT-0000,5", "'   ',6", "inexistente,0", "%,0", "_,0", "!,0"})
    void codigoEsParcialIgnoraMayusculasYEscapaCaracteres(String codigo, int cantidad) throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param("codigo", codigo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(cantidad));
    }

    @ParameterizedTest
    @CsvSource({"BORRADOR,2", "ENVIADA,1", "ACEPTADA,1", "RECHAZADA,1", "VENCIDA,1"})
    void filtraPorEstadoAlmacenadoSinCambiarloPorFecha(String estado, int cantidad) throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param("estado", estado)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(cantidad))
                .andExpect(jsonPath("$.contenido[0].estado").value(estado));
        assertThat(cotizaciones.findById(registros.get(1).getId()).orElseThrow().getEstado()).isEqualTo(EstadoCotizacion.ENVIADA);
    }

    @Test
    void filtrosCombinadosConteoYPaginas() throws Exception {
        List<Long> esperados = List.of(registros.getFirst().getId(), registros.getLast().getId());
        for (int pagina = 0; pagina < 2; pagina++) {
            mvc.perform(get("/api/cotizaciones/buscar").param("codigo", "cot-")
                            .param("clienteId", alfa.getId().toString()).param("oportunidadId", primera.getId().toString())
                            .param("estado", "BORRADOR").param("fechaDesde", "2026-01-01").param("fechaHasta", "2026-01-02")
                            .param("tamano", "1").param("pagina", "" + pagina))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(1))
                    .andExpect(jsonPath("$.contenido[0].id").value(esperados.get(pagina)))
                    .andExpect(jsonPath("$.totalElementos").value(2)).andExpect(jsonPath("$.totalPaginas").value(2))
                    .andExpect(jsonPath("$.primera").value(pagina == 0)).andExpect(jsonPath("$.ultima").value(pagina == 1));
        }
        mvc.perform(get("/api/cotizaciones/buscar").param("clienteId", alfa.getId().toString())
                        .param("oportunidadId", segunda.getId().toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void filtraPorClienteYOportunidadIndividualmente() throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param("clienteId", alfa.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(5));
        mvc.perform(get("/api/cotizaciones/buscar").param("oportunidadId", primera.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(4));
    }

    @ParameterizedTest
    @CsvSource({"fechaDesde,2026-01-03,3", "fechaHasta,2026-01-02,3"})
    void permiteExtremosIndependientes(String campo, String valor, int cantidad) throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param(campo, valor)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(cantidad));
    }

    @Test
    void fechasInclusivasFiltranEmisionYNoVencimiento() throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param("fechaDesde", "2026-01-02").param("fechaHasta", "2026-01-02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get("/api/cotizaciones/buscar").param("fechaDesde", "2026-01-16").param("fechaHasta", "2026-01-20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "codigo", "fechaEmision", "fechaVencimiento", "estado", "subtotal", "igv", "total"})
    void admiteLosCamposDeOrdenPermitidos(String campo) throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param("orden", campo).param("direccion", "desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    void ordenPorTotalDesempataPorIdSinPerderRegistros() throws Exception {
        List<Long> encontrados = new ArrayList<>();
        for (int pagina = 0; pagina < 2; pagina++) {
            String json = mvc.perform(get("/api/cotizaciones/buscar").param("fechaHasta", "2026-01-02")
                            .param("orden", "total").param("pagina", "" + pagina).param("tamano", "2"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(3))
                    .andReturn().getResponse().getContentAsString();
            encontrados.addAll(JsonPath.<List<Number>>read(json, "$.contenido[*].id")
                    .stream().map(Number::longValue).toList());
        }
        assertThat(encontrados).containsExactly(registros.get(0).getId(), registros.get(1).getId(), registros.get(5).getId())
                .doesNotHaveDuplicates();
        mvc.perform(get("/api/cotizaciones/buscar").param("orden", "total").param("direccion", "desc").param("tamano", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].id").value(registros.get(4).getId()));
        mvc.perform(get("/api/cotizaciones/buscar").param("orden", "fechaEmision").param("direccion", "desc").param("tamano", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].fechaEmision").value("2026-01-05"));
        mvc.perform(get("/api/cotizaciones/buscar").param("orden", "codigo").param("direccion", "desc").param("tamano", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].codigo").value("COT-100000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"clienteId", "oportunidadId"})
    void referenciaInexistenteDevuelvePaginaVacia(String campo) throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param(campo, "999999999")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty()).andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void paginaFueraDelResultadoConservaTotales() throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param("pagina", "99").param("tamano", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(6)).andExpect(jsonPath("$.totalPaginas").value(1));
    }

    static Stream<Arguments> parametrosInvalidos() {
        return Stream.of(
                Arguments.of("pagina", "-1"), Arguments.of("pagina", "abc"), Arguments.of("pagina", "2147483648"),
                Arguments.of("pagina", "2147483647"), Arguments.of("tamano", "0"), Arguments.of("tamano", "101"),
                Arguments.of("orden", "oportunidad.cliente.ruc"), Arguments.of("direccion", "lateral"),
                Arguments.of("codigo", "a".repeat(201)), Arguments.of("estado", "INVALIDO"),
                Arguments.of("clienteId", "0"), Arguments.of("clienteId", "-1"), Arguments.of("clienteId", "abc"),
                Arguments.of("oportunidadId", "0"), Arguments.of("oportunidadId", "-1"), Arguments.of("oportunidadId", "abc"),
                Arguments.of("fechaDesde", "31/01/2026"), Arguments.of("fechaHasta", "2026-02-30"),
                Arguments.of("fechaDesde", "invalida"));
    }

    @ParameterizedTest
    @MethodSource("parametrosInvalidos")
    void parametrosInvalidosDevuelven400(String campo, String valor) throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param(campo, valor))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void rangoInvertidoDevuelve400() throws Exception {
        mvc.perform(get("/api/cotizaciones/buscar").param("fechaDesde", "2026-02-01").param("fechaHasta", "2026-01-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void conservaConsultasExistentesYDescargaPdf() throws Exception {
        Cotizacion c = registros.getFirst();
        mvc.perform(get("/api/cotizaciones")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
        mvc.perform(get("/api/cotizaciones/{id}", c.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(c.getCodigo()));
        mvc.perform(get("/api/cotizaciones/codigo/{codigo}", c.getCodigo())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId()));
        byte[] pdf = mvc.perform(get("/api/cotizaciones/{codigo}/pdf", c.getCodigo()))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
