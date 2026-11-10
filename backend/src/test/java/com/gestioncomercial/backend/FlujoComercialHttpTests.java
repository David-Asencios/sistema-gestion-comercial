package com.gestioncomercial.backend;

import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:flujo_http_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "server.address=127.0.0.1"
})
@ActiveProfiles("test")
class FlujoComercialHttpTests {
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private record Respuesta(int status, String tipo, byte[] cuerpo) {
        String json() { return new String(cuerpo, StandardCharsets.UTF_8); }
        long id() { return ((Number) JsonPath.read(json(), "$.id")).longValue(); }
        String codigo() { return JsonPath.read(json(), "$.codigo"); }
    }

    private record Recorrido(long cotizacion, String codigo) {
    }

    private Respuesta enviar(String metodo, String ruta, String cuerpo, int estadoEsperado) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + ruta))
                .timeout(Duration.ofSeconds(15));
        if (cuerpo != null) request.header("Content-Type", "application/json");
        request.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
        HttpResponse<byte[]> response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).withFailMessage("%s %s: se esperaba %s; se recibió %s: %s",
                metodo, ruta, estadoEsperado, response.statusCode(), new String(response.body(), StandardCharsets.UTF_8))
                .isEqualTo(estadoEsperado);
        return new Respuesta(response.statusCode(), response.headers().firstValue("Content-Type").orElse(""), response.body());
    }

    private String detalle(long cotizacion, String concepto, long conceptoId, int cantidad, int precio, String tipo, int descuento) {
        return """
                {"cotizacion":{"id":%d},"%s":{"id":%d},"descripcion":"%s HTTP",
                 "cantidad":%d,"precioUnitario":%d,"tipoDescuento":"%s","valorDescuento":%d}
                """.formatted(cotizacion, concepto, conceptoId, concepto.equals("servicio") ? "Alquiler" : "Producto",
                cantidad, precio, tipo, descuento);
    }

    private void comprobarImportes(Respuesta respuesta) {
        assertThat(((Number) JsonPath.read(respuesta.json(), "$.subtotal")).doubleValue()).isEqualTo(1500);
        assertThat(((Number) JsonPath.read(respuesta.json(), "$.igv")).doubleValue()).isEqualTo(270);
        assertThat(((Number) JsonPath.read(respuesta.json(), "$.total")).doubleValue()).isEqualTo(1770);
    }

    /** Todo el registro y seguimiento usa HTTP real; cada servicio confirma su propia transacción. */
    private Recorrido registrarYEnviarCotizacionMixta() throws Exception {
        long cliente = enviar("POST", "/api/clientes",
                "{\"razonSocial\":\"Cliente flujo HTTP\",\"nombreContacto\":\"Contacto HTTP\"}", 201).id();
        long oportunidad = enviar("POST", "/api/oportunidades", """
                {"titulo":"Oportunidad HTTP","canalOrigen":"WHATSAPP","cliente":{"id":%d}}
                """.formatted(cliente), 201).id();
        long servicio = enviar("POST", "/api/servicios", "{\"nombre\":\"Alquiler HTTP\",\"precioReferencial\":180}", 201).id();
        long producto = enviar("POST", "/api/productos", "{\"nombre\":\"Producto HTTP\",\"precioReferencial\":250}", 201).id();
        Respuesta creada = enviar("POST", "/api/cotizaciones", "{\"oportunidad\":{\"id\":" + oportunidad + "}}", 201);
        long cotizacion = creada.id();
        assertThat(creada.codigo()).matches("COT-[0-9]{5,}");
        enviar("PATCH", "/api/cotizaciones/" + cotizacion + "/estado", "\"ENVIADA\"", 400);

        Respuesta alquiler = enviar("POST", "/api/detalles-cotizacion",
                detalle(cotizacion, "servicio", servicio, 7, 180, "MONTO", 30), 201);
        assertThat(((Number) JsonPath.read(alquiler.json(), "$.descuentoTotal")).doubleValue()).isEqualTo(210);
        assertThat(((Number) JsonPath.read(alquiler.json(), "$.subtotal")).doubleValue()).isEqualTo(1050);
        Respuesta venta = enviar("POST", "/api/detalles-cotizacion",
                detalle(cotizacion, "producto", producto, 2, 250, "PORCENTAJE", 10), 201);
        assertThat(((Number) JsonPath.read(venta.json(), "$.descuentoTotal")).doubleValue()).isEqualTo(50);
        assertThat(((Number) JsonPath.read(venta.json(), "$.subtotal")).doubleValue()).isEqualTo(450);
        enviar("POST", "/api/detalles-cotizacion", detalle(cotizacion, "servicio", servicio, 1, 180, "MONTO", 181), 400);
        comprobarImportes(enviar("GET", "/api/cotizaciones/" + cotizacion, null, 200));
        Respuesta lista = enviar("GET", "/api/detalles-cotizacion/cotizacion/" + creada.codigo(), null, 200);
        assertThat(JsonPath.<List<?>>read(lista.json(), "$")).hasSize(2);
        assertThat(((Number) JsonPath.read(enviar("GET", "/api/servicios/" + servicio, null, 200).json(), "$.precioReferencial")).doubleValue()).isEqualTo(180);
        assertThat(((Number) JsonPath.read(enviar("GET", "/api/productos/" + producto, null, 200).json(), "$.precioReferencial")).doubleValue()).isEqualTo(250);

        Respuesta pdf = enviar("GET", "/api/cotizaciones/" + creada.codigo() + "/pdf", null, 200);
        assertThat(pdf.tipo()).contains("application/pdf");
        try (PDDocument documento = PDDocument.load(pdf.cuerpo())) {
            assertThat(new PDFTextStripper().getText(documento))
                    .contains("Cliente flujo HTTP", "Alquiler HTTP", "Producto HTTP", "DESCUENTO", "S/ 1500.00", "S/ 1770.00");
        }

        enviar("PATCH", "/api/cotizaciones/" + cotizacion + "/estado", "\"ENVIADA\"", 200);
        enviar("PUT", "/api/cotizaciones/" + cotizacion,
                "{\"oportunidad\":{\"id\":" + oportunidad + "},\"vigenciaDias\":30}", 400);
        enviar("PUT", "/api/detalles-cotizacion/" + alquiler.id(), detalle(cotizacion, "servicio", servicio, 7, 180, "MONTO", 40), 400);
        enviar("DELETE", "/api/detalles-cotizacion/" + alquiler.id(), null, 400);
        enviar("POST", "/api/detalles-cotizacion", detalle(cotizacion, "servicio", servicio, 1, 180, "NINGUNO", 0), 400);
        comprobarImportes(enviar("GET", "/api/cotizaciones/" + cotizacion, null, 200));
        return new Recorrido(cotizacion, creada.codigo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACEPTADA", "RECHAZADA"})
    void recorridoCompletoHastaEstadoFinal(String estadoFinal) throws Exception {
        Recorrido recorrido = registrarYEnviarCotizacionMixta();
        enviar("PATCH", "/api/cotizaciones/" + recorrido.cotizacion() + "/estado", "\"" + estadoFinal + "\"", 200);
        Respuesta consulta = enviar("GET", "/api/cotizaciones/codigo/" + recorrido.codigo(), null, 200);
        assertThat(JsonPath.<String>read(consulta.json(), "$.estado")).isEqualTo(estadoFinal);
        comprobarImportes(consulta);
        enviar("PATCH", "/api/cotizaciones/" + recorrido.cotizacion() + "/estado", "\"ENVIADA\"", 400);
    }

    @Test
    void enviadaQueVenceDespuesNoPuedeAceptarseYSeMarcaVencida() throws Exception {
        Recorrido recorrido = registrarYEnviarCotizacionMixta();
        enviar("PATCH", "/api/cotizaciones/" + recorrido.cotizacion() + "/estado", "\"VENCIDA\"", 400);
        // Fixture temporal, exclusivamente en H2: representa una enviada cuya vigencia terminó ayer.
        assertThat(jdbc.update("UPDATE cotizaciones SET fecha_emision=?, fecha_vencimiento=? WHERE id=?",
                java.sql.Date.valueOf(LocalDate.now().minusDays(16)),
                java.sql.Date.valueOf(LocalDate.now().minusDays(1)), recorrido.cotizacion())).isEqualTo(1);
        enviar("PATCH", "/api/cotizaciones/" + recorrido.cotizacion() + "/estado", "\"ACEPTADA\"", 400);
        Respuesta enviada = enviar("GET", "/api/cotizaciones/" + recorrido.cotizacion(), null, 200);
        assertThat(JsonPath.<String>read(enviada.json(), "$.estado")).isEqualTo("ENVIADA");
        Respuesta vencida = enviar("PATCH", "/api/cotizaciones/" + recorrido.cotizacion() + "/estado", "\"VENCIDA\"", 200);
        assertThat(JsonPath.<String>read(vencida.json(), "$.estado")).isEqualTo("VENCIDA");
        comprobarImportes(vencida);
        enviar("PATCH", "/api/cotizaciones/" + recorrido.cotizacion() + "/estado", "\"ENVIADA\"", 400);
    }

    @Test
    @Timeout(90)
    @EnabledIfSystemProperty(named = "run.postman", matches = "true")
    void ejecutaLaColeccionPostmanConSusScriptsReales() throws Exception {
        Path runner = Path.of("target", "newman", "node_modules", "newman", "bin", "newman.js").toAbsolutePath();
        assertThat(runner).as("Instalar Newman en target/newman según postman/README.md").exists();
        Path coleccion = Path.of("..", "postman", "flujo-comercial.postman_collection.json").toAbsolutePath().normalize();
        Path entorno = Path.of("..", "postman", "local.postman_environment.json").toAbsolutePath().normalize();
        Path resultados = Path.of("target", "postman-results").toAbsolutePath();
        Files.createDirectories(resultados);
        Path log = resultados.resolve("newman.log");
        Process process = new ProcessBuilder("node", runner.toString(), "run", coleccion.toString(),
                "--environment", entorno.toString(), "--env-var", "baseUrl=http://127.0.0.1:" + port,
                "--bail", "--timeout-request", "15000", "--reporters", "cli,json",
                "--reporter-json-export", resultados.resolve("newman-report.json").toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertThat(process.waitFor(60, TimeUnit.SECONDS)).as("La colección debe terminar en un minuto").isTrue();
            assertThat(process.exitValue()).withFailMessage("Falló la colección Postman:\n%s", Files.readString(log)).isZero();
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
