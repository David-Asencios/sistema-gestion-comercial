package com.gestioncomercial.backend;

import com.gestioncomercial.backend.entity.*;
import com.gestioncomercial.backend.repository.*;
import com.gestioncomercial.backend.service.CotizacionService;
import com.gestioncomercial.backend.service.DetalleCotizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:validaciones_comerciales_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ValidacionesComercialesIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ClienteRepository clientes;
    @Autowired OportunidadComercialRepository oportunidades;
    @Autowired CotizacionRepository cotizaciones;
    @Autowired ServicioRepository servicios;
    @Autowired CotizacionService cotizacionService;
    @Autowired DetalleCotizacionService detalleService;

    private Cliente cliente;
    private OportunidadComercial oportunidad;
    private Cotizacion cotizacion;

    @BeforeEach
    void prepararDatos() {
        cliente = clientes.save(cliente("20123456789"));
        oportunidad = new OportunidadComercial();
        oportunidad.setTitulo("Oportunidad de prueba");
        oportunidad.setCanalOrigen(CanalOrigen.WHATSAPP);
        oportunidad.setCliente(cliente);
        oportunidad = oportunidades.save(oportunidad);
        Cotizacion entrada = new Cotizacion();
        entrada.setOportunidad(oportunidad);
        cotizacion = cotizacionService.guardarCotizacion(entrada);
    }

    private Cliente cliente(String ruc) {
        Cliente nuevo = new Cliente();
        nuevo.setRuc(ruc);
        nuevo.setRazonSocial("Cliente de prueba");
        nuevo.setNombreContacto("Contacto");
        return nuevo;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void permiteRegistrarVariosClientesSinRuc(String ruc) throws Exception {
        String campoRuc = ruc == null ? "" : ",\"ruc\":\"" + ruc + "\"";
        String cuerpo = "{\"razonSocial\":\"Cliente sin RUC\",\"nombreContacto\":\"Contacto\"" + campoRuc + "}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.ruc").doesNotExist());
        }
        assertThat(clientes.findAll().stream().filter(c -> c.getRuc() == null).count()).isEqualTo(2);
    }

    @Test
    void actualizaClienteSinRucYPermiteAsignarloYQuitarlo() throws Exception {
        Cliente sinRuc = clientes.save(cliente(null));
        String cuerpo = "{\"razonSocial\":\"Nombre actualizado\",\"nombreContacto\":\"Contacto\"%s}";
        mvc.perform(put("/api/clientes/{id}", sinRuc.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.formatted("")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ruc").doesNotExist())
                .andExpect(jsonPath("$.razonSocial").value("Nombre actualizado"));
        mvc.perform(put("/api/clientes/{id}", sinRuc.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.formatted(",\"ruc\":\"20987654321\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ruc").value("20987654321"));
        mvc.perform(put("/api/clientes/{id}", sinRuc.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo.formatted(",\"ruc\":\"   \"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ruc").doesNotExist());
        assertThat(clientes.findById(sinRuc.getId()).orElseThrow().getRuc()).isNull();
    }

    @Test
    void conservaRucPropioYRechazaDuplicadosCon409() throws Exception {
        String cuerpo = "{\"razonSocial\":\"Cliente de prueba\",\"nombreContacto\":\"Contacto\",\"ruc\":\"20123456789\"}";
        mvc.perform(put("/api/clientes/{id}", cliente.getId()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());
        long cantidad = clientes.count();
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").exists());
        Cliente otro = clientes.save(cliente(null));
        mvc.perform(put("/api/clientes/{id}", otro.getId()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
        assertThat(clientes.count()).isEqualTo(cantidad + 1);
        assertThat(clientes.findById(otro.getId()).orElseThrow().getRuc()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"cliente\":null", ",\"cliente\":{}", ",\"cliente\":{\"id\":null}",
            ",\"cliente\":{\"id\":0}", ",\"cliente\":{\"id\":-1}"})
    void clienteIncompletoDevuelve400AlCrearYActualizarOportunidad(String referencia) throws Exception {
        String cuerpo = "{\"titulo\":\"Cambio inválido\",\"canalOrigen\":\"WHATSAPP\"" + referencia + "}";
        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath(
                        referencia.isEmpty() || referencia.equals(",\"cliente\":null") ? "$.cliente" : "$['cliente.id']").exists());
        mvc.perform(put("/api/oportunidades/{id}", oportunidad.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
        assertThat(oportunidades.count()).isEqualTo(1);
        assertThat(oportunidades.findById(oportunidad.getId()).orElseThrow().getTitulo())
                .isEqualTo("Oportunidad de prueba");
    }

    @Test
    void clienteInexistenteDevuelve404AlCrearYActualizarOportunidad() throws Exception {
        String cuerpo = "{\"titulo\":\"Cambio inválido\",\"canalOrigen\":\"WHATSAPP\",\"cliente\":{\"id\":99999999}}";
        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/oportunidades/{id}", oportunidad.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isNotFound());
        assertThat(oportunidades.count()).isEqualTo(1);
        assertThat(oportunidades.findById(oportunidad.getId()).orElseThrow().getCliente().getId())
                .isEqualTo(cliente.getId());
    }

    @Test
    void oportunidadCargaClienteRealSinModificarSusDatosDesdeLaReferencia() throws Exception {
        String cuerpo = """
                {"titulo":"Oportunidad válida","canalOrigen":"WHATSAPP",
                 "cliente":{"id":%d,"razonSocial":"Nombre falso"}}
                """.formatted(cliente.getId());
        mvc.perform(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cliente.razonSocial").value("Cliente de prueba"));
        mvc.perform(put("/api/oportunidades/{id}", oportunidad.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.razonSocial").value("Cliente de prueba"));
        assertThat(clientes.findById(cliente.getId()).orElseThrow().getRazonSocial()).isEqualTo("Cliente de prueba");
    }

    private void agregarDetalle(boolean descuentoCompleto) {
        Servicio servicio = new Servicio();
        servicio.setNombre("Servicio de prueba");
        servicio.setPrecioReferencial(new BigDecimal("100.00"));
        servicio = servicios.save(servicio);
        DetalleCotizacion detalle = new DetalleCotizacion();
        detalle.setCotizacion(cotizacion);
        detalle.setServicio(servicio);
        detalle.setDescripcion("Servicio cotizado");
        detalle.setCantidad(BigDecimal.ONE);
        detalle.setPrecioUnitario(new BigDecimal("100.00"));
        if (descuentoCompleto) {
            detalle.setTipoDescuento(DetalleCotizacion.TipoDescuento.PORCENTAJE);
            detalle.setValorDescuento(new BigDecimal("100"));
        }
        detalleService.guardarDetalle(detalle);
    }

    private void configurarEstadoYVencimiento(EstadoCotizacion estado, Integer diasDesdeHoy) {
        cotizacion.setEstado(estado);
        cotizacion.setFechaVencimiento(diasDesdeHoy == null ? null : LocalDate.now().plusDays(diasDesdeHoy));
        cotizaciones.saveAndFlush(cotizacion);
    }

    private org.springframework.test.web.servlet.ResultActions cambiarEstado(String estado) throws Exception {
        return mvc.perform(patch("/api/cotizaciones/{id}/estado", cotizacion.getId())
                .contentType(MediaType.APPLICATION_JSON).content("\"" + estado + "\""));
    }

    private void comprobarEstado(EstadoCotizacion estado) {
        assertThat(cotizaciones.findById(cotizacion.getId()).orElseThrow().getEstado()).isEqualTo(estado);
    }

    @Test
    void noEnviaCotizacionVaciaNiCambiaSuEstado() throws Exception {
        cambiarEstado("ENVIADA").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede enviar una cotización sin detalles"));
        comprobarEstado(EstadoCotizacion.BORRADOR);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void permiteEnviarConDetallesInclusoConDescuentoCompleto(boolean descuentoCompleto) throws Exception {
        agregarDetalle(descuentoCompleto);
        configurarEstadoYVencimiento(EstadoCotizacion.BORRADOR, 0);
        cambiarEstado("ENVIADA").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ENVIADA"));
        comprobarEstado(EstadoCotizacion.ENVIADA);
    }

    @ParameterizedTest
    @CsvSource({"BORRADOR,ENVIADA", "ENVIADA,ACEPTADA"})
    void noEnviaNiAceptaUnaCotizacionVencida(EstadoCotizacion original, String destino) throws Exception {
        agregarDetalle(false);
        configurarEstadoYVencimiento(original, -1);
        cambiarEstado(destino).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede enviar ni aceptar una cotización vencida"));
        comprobarEstado(original);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void aceptaCotizacionHastaSuFechaDeVencimientoInclusive(int diasDesdeHoy) throws Exception {
        agregarDetalle(false);
        configurarEstadoYVencimiento(EstadoCotizacion.ENVIADA, diasDesdeHoy);
        cambiarEstado("ACEPTADA").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACEPTADA"));
        comprobarEstado(EstadoCotizacion.ACEPTADA);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void noMarcaVencidaAntesDeTiempo(int diasDesdeHoy) throws Exception {
        configurarEstadoYVencimiento(EstadoCotizacion.ENVIADA, diasDesdeHoy);
        cambiarEstado("VENCIDA").andExpect(status().isBadRequest());
        comprobarEstado(EstadoCotizacion.ENVIADA);
    }

    @Test
    void permiteMarcarVencidaCuandoSuFechaYaPaso() throws Exception {
        configurarEstadoYVencimiento(EstadoCotizacion.ENVIADA, -1);
        cambiarEstado("VENCIDA").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("VENCIDA"));
        comprobarEstado(EstadoCotizacion.VENCIDA);
    }

    @ParameterizedTest
    @CsvSource({"BORRADOR,ENVIADA", "ENVIADA,ACEPTADA", "ENVIADA,VENCIDA"})
    void fechaDeVencimientoAusenteProduce400(EstadoCotizacion original, String destino) throws Exception {
        agregarDetalle(false);
        configurarEstadoYVencimiento(original, null);
        cambiarEstado(destino).andExpect(status().isBadRequest());
        comprobarEstado(original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACEPTADA", "RECHAZADA", "VENCIDA"})
    void estadosFinalesNoSeReabren(EstadoCotizacion estado) throws Exception {
        configurarEstadoYVencimiento(estado, -1);
        cambiarEstado("ENVIADA").andExpect(status().isBadRequest());
        comprobarEstado(estado);
    }

    @Test
    void conservaRechazoDeCotizacionEnviadaAunqueSuFechaHayaPasado() throws Exception {
        configurarEstadoYVencimiento(EstadoCotizacion.ENVIADA, -1);
        cambiarEstado("RECHAZADA").andExpect(status().isOk());
        comprobarEstado(EstadoCotizacion.RECHAZADA);
    }
}
