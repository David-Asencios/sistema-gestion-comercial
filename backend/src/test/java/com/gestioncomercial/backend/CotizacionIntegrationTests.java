package com.gestioncomercial.backend;

import com.gestioncomercial.backend.entity.*;
import com.gestioncomercial.backend.entity.DetalleCotizacion.TipoDescuento;
import com.gestioncomercial.backend.repository.*;
import com.gestioncomercial.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gestion_comercial_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CotizacionIntegrationTests {
    @Autowired ClienteRepository clientes;
    @Autowired OportunidadComercialRepository oportunidades;
    @Autowired CotizacionRepository cotizaciones;
    @Autowired DetalleCotizacionRepository detalles;
    @Autowired ServicioRepository servicios;
    @Autowired ProductoRepository productos;
    @Autowired CotizacionService cotizacionService;
    @Autowired DetalleCotizacionService detalleService;
    @Autowired CotizacionPdfService pdfService;
    @Autowired MockMvc mvc;

    private OportunidadComercial oportunidad;
    private Cotizacion cotizacion;
    private Servicio servicio;
    private Producto producto;

    @BeforeEach
    void prepararBaseAislada() {
        detalles.deleteAll();
        cotizaciones.deleteAll();
        oportunidades.deleteAll();
        clientes.deleteAll();
        servicios.deleteAll();
        productos.deleteAll();
        Cliente cliente = new Cliente();
        cliente.setRazonSocial("Cliente de pruebas");
        cliente.setNombreContacto("Contacto de pruebas");
        cliente.setRuc("20123456789");
        cliente.setCorreo("prueba@example.com");
        cliente.setTelefono("999999999");
        cliente.setDireccion("Lima, Perú");
        cliente = clientes.save(cliente);
        oportunidad = new OportunidadComercial();
        oportunidad.setTitulo("Alquiler de equipo");
        oportunidad.setCanalOrigen(CanalOrigen.WHATSAPP);
        oportunidad.setCliente(cliente);
        oportunidad = oportunidades.save(oportunidad);
        servicio = new Servicio();
        servicio.setNombre("Alquiler por día");
        servicio.setPrecioReferencial(new BigDecimal("180.00"));
        servicio = servicios.save(servicio);
        producto = new Producto();
        producto.setNombre("Equipo");
        producto.setPrecioReferencial(new BigDecimal("180.00"));
        producto = productos.save(producto);
        cotizacion = nuevaCotizacion();
    }

    private Cotizacion nuevaCotizacion() {
        Cotizacion nueva = new Cotizacion();
        nueva.setOportunidad(oportunidad);
        nueva.setFechaEmision(LocalDate.now());
        nueva.setVigenciaDias(30);
        nueva.setAlcanceServicio("Alquiler durante siete días.\nIncluye verificación del equipo.");
        return cotizacionService.guardarCotizacion(nueva);
    }

    private DetalleCotizacion detalle(TipoDescuento tipo, String valor) {
        DetalleCotizacion detalle = new DetalleCotizacion();
        detalle.setCotizacion(cotizacion);
        detalle.setServicio(servicio);
        detalle.setDescripcion("Alquiler de equipo por siete días");
        detalle.setCantidad(new BigDecimal("7.00"));
        detalle.setPrecioUnitario(new BigDecimal("180.00"));
        detalle.setTipoDescuento(tipo);
        detalle.setValorDescuento(valor == null ? null : new BigDecimal(valor));
        return detalle;
    }

    @ParameterizedTest
    @CsvSource({
            "NINGUNO,0,0,1260", "MONTO,30,210,1050", "PORCENTAJE,10,126,1134",
            "MONTO,180,1260,0", "PORCENTAJE,100,1260,0", "PORCENTAJE,0,0,1260"
    })
    void calculaDescuentosYTotales(TipoDescuento tipo, String valor, String descuento, String subtotal) {
        DetalleCotizacion guardado = detalleService.guardarDetalle(detalle(tipo, valor));
        assertThat(guardado.getDescuentoTotal()).isEqualByComparingTo(descuento);
        assertThat(guardado.getSubtotal()).isEqualByComparingTo(subtotal);
        Cotizacion actual = cotizacionService.buscarCotizacionPorId(cotizacion.getId());
        assertThat(actual.getSubtotal()).isEqualByComparingTo(subtotal);
        BigDecimal igv = new BigDecimal(subtotal).multiply(new BigDecimal("0.18"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(actual.getIgv()).isEqualByComparingTo(igv);
        assertThat(actual.getTotal()).isEqualByComparingTo(new BigDecimal(subtotal).add(igv));
        assertThat(servicios.findById(servicio.getId()).orElseThrow().getPrecioReferencial())
                .isEqualByComparingTo("180");
    }

    @ParameterizedTest
    @CsvSource({"MONTO,-1", "MONTO,180.01", "PORCENTAJE,-0.01", "PORCENTAJE,100.01", "NINGUNO,1"})
    void rechazaDescuentosInvalidosSinPersistir(TipoDescuento tipo, String valor) {
        assertThatThrownBy(() -> detalleService.guardarDetalle(detalle(tipo, valor)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(detalles.count()).isZero();
        assertThat(cotizaciones.findById(cotizacion.getId()).orElseThrow().getTotal()).isZero();
    }

    @Test
    void aplicaValoresPredeterminadosYSobrescribeCalculosRecibidos() {
        DetalleCotizacion entrada = detalle(null, null);
        entrada.setDescuentoUnitario(new BigDecimal("100"));
        entrada.setDescuentoTotal(new BigDecimal("700"));
        entrada.setSubtotal(BigDecimal.ONE);
        DetalleCotizacion guardado = detalleService.guardarDetalle(entrada);
        assertThat(guardado.getTipoDescuento()).isEqualTo(TipoDescuento.NINGUNO);
        assertThat(guardado.getValorDescuento()).isZero();
        assertThat(guardado.getDescuentoUnitario()).isZero();
        assertThat(guardado.getDescuentoTotal()).isZero();
        assertThat(guardado.getSubtotal()).isEqualByComparingTo("1260");
    }

    @Test
    void exigeValorCuandoSeSolicitaDescuento() {
        assertThatThrownBy(() -> detalleService.guardarDetalle(detalle(TipoDescuento.MONTO, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculaProductoYConservaFotografiaDelDetalle() {
        DetalleCotizacion entrada = detalle(TipoDescuento.PORCENTAJE, "10");
        entrada.setServicio(null);
        entrada.setProducto(producto);
        Long id = detalleService.guardarDetalle(entrada).getId();
        producto.setPrecioReferencial(new BigDecimal("250"));
        productos.save(producto);
        DetalleCotizacion actual = detalleService.buscarDetallePorId(id);
        assertThat(actual.getPrecioUnitario()).isEqualByComparingTo("180");
        assertThat(actual.getDescuentoTotal()).isEqualByComparingTo("126");
        assertThat(actual.getSubtotal()).isEqualByComparingTo("1134");
    }

    @Test
    void actualizaDescuentoYEliminacionRecalculaTotales() {
        Long id = detalleService.guardarDetalle(detalle(TipoDescuento.MONTO, "30")).getId();
        DetalleCotizacion actual = detalleService.actualizarDetalleCotizacion(id, detalle(TipoDescuento.PORCENTAJE, "10"));
        assertThat(actual.getTipoDescuento()).isEqualTo(TipoDescuento.PORCENTAJE);
        assertThat(actual.getValorDescuento()).isEqualByComparingTo("10");
        assertThat(actual.getSubtotal()).isEqualByComparingTo("1134");
        assertThat(cotizacionService.buscarCotizacionPorId(cotizacion.getId()).getTotal())
                .isEqualByComparingTo("1338.12");
        detalleService.eliminarDetalle(id);
        assertThat(cotizacionService.buscarCotizacionPorId(cotizacion.getId()).getTotal()).isZero();
    }

    @Test
    void moverDetalleRecalculaAmbasCotizaciones() {
        Long id = detalleService.guardarDetalle(detalle(TipoDescuento.MONTO, "30")).getId();
        Cotizacion destino = nuevaCotizacion();
        DetalleCotizacion entrada = detalle(TipoDescuento.MONTO, "30");
        entrada.setCotizacion(destino);
        detalleService.actualizarDetalleCotizacion(id, entrada);
        assertThat(cotizacionService.buscarCotizacionPorId(cotizacion.getId()).getTotal()).isZero();
        assertThat(cotizacionService.buscarCotizacionPorId(destino.getId()).getTotal()).isEqualByComparingTo("1239");
    }

    @Test
    void cotizacionEnviadaImpideCrearActualizarYEliminarDetalles() {
        Long id = detalleService.guardarDetalle(detalle(TipoDescuento.MONTO, "30")).getId();
        cotizacionService.actualizarEstadoCotizacion(cotizacion.getId(), EstadoCotizacion.ENVIADA);
        assertThatThrownBy(() -> detalleService.guardarDetalle(detalle(TipoDescuento.MONTO, "30")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> detalleService.actualizarDetalleCotizacion(id, detalle(TipoDescuento.MONTO, "40")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> detalleService.eliminarDetalle(id)).isInstanceOf(IllegalArgumentException.class);
        assertThat(detalleService.buscarDetallePorId(id).getDescuentoTotal()).isEqualByComparingTo("210");
    }

    @Test
    void redondeaPorUnidadYMantieneBrutoMenosDescuentoIgualAlImporte() {
        DetalleCotizacion entrada = detalle(TipoDescuento.PORCENTAJE, "33.33");
        entrada.setPrecioUnitario(new BigDecimal("0.05"));
        entrada.setCantidad(new BigDecimal("1.50"));
        DetalleCotizacion actual = detalleService.guardarDetalle(entrada);
        assertThat(actual.getDescuentoUnitario()).isEqualByComparingTo("0.02");
        assertThat(actual.getDescuentoTotal()).isEqualByComparingTo("0.03");
        assertThat(actual.getSubtotal()).isEqualByComparingTo("0.05");
    }

    @Test
    void rechazaPrecisionExcesivaYDesbordamientoConRollback() {
        DetalleCotizacion entrada = detalle(TipoDescuento.MONTO, "0.001");
        assertThatThrownBy(() -> detalleService.guardarDetalle(entrada)).isInstanceOf(IllegalArgumentException.class);
        entrada.setValorDescuento(BigDecimal.ZERO);
        entrada.setCantidad(new BigDecimal("99999999.99"));
        entrada.setPrecioUnitario(new BigDecimal("9999999999.99"));
        assertThatThrownBy(() -> detalleService.guardarDetalle(entrada)).isInstanceOf(IllegalArgumentException.class);
        entrada.setCantidad(BigDecimal.ONE);
        // La línea cabe en DECIMAL(12,2), pero el total con IGV no: debe revertirse toda la transacción.
        assertThatThrownBy(() -> detalleService.guardarDetalle(entrada)).isInstanceOf(IllegalArgumentException.class);
        assertThat(detalles.count()).isZero();
    }

    private String jsonDetalle(String extra) {
        return """
                {"cotizacion":{"id":%d},"servicio":{"id":%d},
                 "descripcion":"Alquiler de equipo","cantidad":7,"precioUnitario":180,
                 "tipoDescuento":"MONTO","valorDescuento":30%s}
                """.formatted(cotizacion.getId(), servicio.getId(), extra);
    }

    @Test
    void contratoHttpConservaReferenciasYCalculaEnServidor() throws Exception {
        mvc.perform(post("/api/detalles-cotizacion").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDetalle("")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(1050))
                .andExpect(jsonPath("$.descuentoTotal").value(210))
                .andExpect(jsonPath("$.cotizacion.total").value(1239))
                .andExpect(jsonPath("$.cotizacion.oportunidad.cliente.razonSocial").value("Cliente de pruebas"));
        mvc.perform(get("/api/cotizaciones/{id}", cotizacion.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1239));
        mvc.perform(get("/api/detalles-cotizacion/cotizacion/{codigo}", cotizacion.getCodigo()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].descuentoTotal").value(210));
    }

    @Test
    void validaHttpAntesDePersistir() throws Exception {
        for (String json : List.of(
                jsonDetalle("").replace("\"cantidad\":7", "\"cantidad\":0"),
                jsonDetalle("").replace("\"valorDescuento\":30", "\"valorDescuento\":181"),
                jsonDetalle("").replace("\"MONTO\"", "\"INVALIDO\""),
                jsonDetalle(",\"producto\":{\"id\":" + producto.getId() + "}"),
                jsonDetalle("").replace("\"cotizacion\":{\"id\":" + cotizacion.getId() + "}", "\"cotizacion\":null"),
                jsonDetalle("").replace("\"valorDescuento\":30", "\"valorDescuento\":0.001"))) {
            mvc.perform(post("/api/detalles-cotizacion").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        assertThat(detalles.count()).isZero();
    }

    @Test
    void camposCalculadosEnJsonNoControlanLaPersistencia() throws Exception {
        mvc.perform(post("/api/detalles-cotizacion").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDetalle(",\"id\":999999,\"subtotal\":1,\"descuentoTotal\":999,\"descuentoUnitario\":999")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(1050))
                .andExpect(jsonPath("$.descuentoTotal").value(210))
                .andExpect(jsonPath("$.descuentoUnitario").value(30));
        assertThat(detalles.existsById(999999L)).isFalse();
    }

    @Test
    void contratoCotizacionAplicaPredeterminadosYPreservaVigenciaAlActualizar() throws Exception {
        String cuerpo = "{\"oportunidad\":{\"id\":" + oportunidad.getId() + "}}";
        mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.vigenciaDias").value(15))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.oportunidad.cliente.razonSocial").value("Cliente de pruebas"));
        mvc.perform(put("/api/cotizaciones/{id}", cotizacion.getId()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vigenciaDias").value(30))
                .andExpect(jsonPath("$.fechaEmision").value(cotizacion.getFechaEmision().toString()));
    }

    @Test
    void generaPdfConDescuentoYVigenciaReal() throws Exception {
        detalleService.guardarDetalle(detalle(TipoDescuento.MONTO, "30"));
        byte[] pdf = mvc.perform(get("/api/cotizaciones/{codigo}/pdf", cotizacion.getCodigo()))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + cotizacion.getCodigo() + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();
        try (PDDocument documento = PDDocument.load(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertThat(texto).contains("DESCUENTO", "30 días", "S/ 210.00", "S/ 1050.00", "S/ 1239.00");
            assertThat(documento.getNumberOfPages()).isEqualTo(1);
            comprobarLogo(documento);
            comprobarPaginacion(documento);
        }
        Path salida = Path.of("target", "pdf-verification");
        Files.createDirectories(salida);
        Files.write(salida.resolve("cotizacion-descuento.pdf"), pdf);
    }

    @Test
    void pdfInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/cotizaciones/NO-EXISTE/pdf")).andExpect(status().isNotFound());
    }

    @Test
    void generaPdfDeVariasPaginasSinPerderConceptos() throws Exception {
        for (int i = 1; i <= 35; i++) {
            DetalleCotizacion entrada = detalle(TipoDescuento.MONTO, "30");
            entrada.setDescripcion("Concepto " + i + " - Alquiler de equipo con verificación y asistencia técnica");
            detalleService.guardarDetalle(entrada);
        }
        byte[] pdf = pdfService.generarPdf(cotizacion.getCodigo());
        try (PDDocument documento = PDDocument.load(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertThat(documento.getNumberOfPages()).isGreaterThan(1);
            comprobarLogo(documento);
            comprobarPaginacion(documento);
            assertThat(texto).contains("Concepto 1 -", "Concepto 35 -", "S/ 36750.00", "S/ 43365.00");
            PDFTextStripper pagina = new PDFTextStripper();
            pagina.setStartPage(2);
            pagina.setEndPage(2);
            assertThat(pagina.getText(documento)).contains("DESCUENTO");
            for (int numero = 1; numero <= documento.getNumberOfPages(); numero++) {
                pagina.setStartPage(numero);
                pagina.setEndPage(numero);
                assertThat(pagina.getText(documento)).contains("www.incovec.com");
            }
        }
        Path salida = Path.of("target", "pdf-verification");
        Files.createDirectories(salida);
        Files.write(salida.resolve("cotizacion-varias-paginas.pdf"), pdf);
    }

    @ParameterizedTest
    @ValueSource(ints = {12, 80})
    void alcancesExtensosFluyenYDatosBancariosPermanecenJuntos(int condiciones) throws Exception {
        StringBuilder alcance = new StringBuilder();
        for (int i = 1; i <= condiciones; i++) {
            alcance.append(String.format("Condición %03d: ", i))
                    .append("El servicio incluye la inspección técnica del equipo, la coordinación de acceso y la entrega del informe. ")
                    .append("El cliente facilitará las condiciones de seguridad y los permisos necesarios para realizar los trabajos.\n");
        }
        cotizacion.setAlcanceServicio(alcance.toString());
        cotizacionService.actualizarCotizacion(cotizacion.getId(), cotizacion);
        detalleService.guardarDetalle(detalle(TipoDescuento.MONTO, "30"));
        byte[] pdf = pdfService.generarPdf(cotizacion.getCodigo());
        try (PDDocument documento = PDDocument.load(pdf)) {
            assertThat(documento.getNumberOfPages()).isGreaterThan(1);
            comprobarLogo(documento);
            comprobarPaginacion(documento);
            String completo = new PDFTextStripper().getText(documento);
            for (int i = 1; i <= condiciones; i++) {
                assertThat(completo).contains(String.format("Condición %03d:", i));
            }
            int paginaBancos = -1;
            int paginaUltimoAlcance = -1;
            PDFTextStripper pagina = new PDFTextStripper();
            for (int numero = 1; numero <= documento.getNumberOfPages(); numero++) {
                pagina.setStartPage(numero);
                pagina.setEndPage(numero);
                String texto = pagina.getText(documento);
                if (texto.contains(String.format("Condición %03d:", condiciones))) paginaUltimoAlcance = numero;
                if (texto.contains("DATOS BANCARIOS")) {
                    assertThat(paginaBancos).isEqualTo(-1);
                    paginaBancos = numero;
                    assertThat(texto).contains("INTERBANK", "BANCO DE LA NACIÓN", "Titular: INCOVEC", "Atentamente");
                }
            }
            assertThat(paginaBancos).isGreaterThan(1).isGreaterThanOrEqualTo(paginaUltimoAlcance);
        }
        Path salida = Path.of("target", "pdf-verification");
        Files.createDirectories(salida);
        Files.write(salida.resolve("cotizacion-alcances-" + condiciones + ".pdf"), pdf);
    }

    private void comprobarPaginacion(PDDocument documento) throws Exception {
        PDFTextStripper pagina = new PDFTextStripper();
        // Los contadores se dibujan al final del flujo PDF; leer en el orden visual de la página.
        pagina.setSortByPosition(true);
        for (int numero = 1; numero <= documento.getNumberOfPages(); numero++) {
            pagina.setStartPage(numero);
            pagina.setEndPage(numero);
            assertThat(pagina.getText(documento))
                    .contains("Página " + numero + " de " + documento.getNumberOfPages(), "www.incovec.com");
        }
    }

    @Test
    void alcanceSinSaltosManualesTambienPuedeOcuparVariasPaginas() throws Exception {
        cotizacion.setAlcanceServicio("INICIOALCANCE "
                + "El servicio incluye inspección, coordinación de acceso, verificación de seguridad y entrega del informe técnico. ".repeat(200)
                + "FINALALCANCE");
        cotizacionService.actualizarCotizacion(cotizacion.getId(), cotizacion);
        detalleService.guardarDetalle(detalle(TipoDescuento.MONTO, "30"));
        try (PDDocument documento = PDDocument.load(pdfService.generarPdf(cotizacion.getCodigo()))) {
            assertThat(documento.getNumberOfPages()).isGreaterThan(1);
            comprobarPaginacion(documento);
            assertThat(new PDFTextStripper().getText(documento)).contains("INICIOALCANCE", "FINALALCANCE", "DATOS BANCARIOS");
        }
    }

    private void comprobarLogo(PDDocument documento) throws Exception {
        boolean logoEncontrado = false;
        var recursos = documento.getPage(0).getResources();
        for (var nombre : recursos.getXObjectNames()) {
            if (recursos.getXObject(nombre) instanceof PDImageXObject imagen
                    && imagen.getWidth() == 592 && imagen.getHeight() == 599) {
                assertThat(imagen.getSoftMask()).as("El logo conserva su transparencia").isNotNull();
                logoEncontrado = true;
            }
        }
        assertThat(logoEncontrado).as("El PDF incluye el logo original de INCOVEC").isTrue();
    }
}
