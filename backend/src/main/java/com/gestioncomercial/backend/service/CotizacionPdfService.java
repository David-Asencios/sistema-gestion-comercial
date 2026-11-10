package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.entity.DetalleCotizacion;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.CotizacionRepository;
import com.gestioncomercial.backend.repository.DetalleCotizacionRepository;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class CotizacionPdfService {

    private final CotizacionRepository cotizacionRepository;
    private final DetalleCotizacionRepository detalleRepository;
    private final TemplateEngine templateEngine;
    private final String logoDataUri;

    public CotizacionPdfService(
            CotizacionRepository cotizacionRepository,
            DetalleCotizacionRepository detalleRepository,
            TemplateEngine templateEngine) {

        this.cotizacionRepository = cotizacionRepository;
        this.detalleRepository = detalleRepository;
        this.templateEngine = templateEngine;
        this.logoDataUri = cargarLogo();
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(String codigo) {

        // 1. Buscar la cotización
        Cotizacion cotizacion = cotizacionRepository
                .buscarPorCodigoConOportunidad(codigo)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe una cotización con el código: " + codigo
                        )
                );

        // 2. Obtener los detalles de la cotización
        List<DetalleCotizacion> detalles =
                detalleRepository.listarPorCodigoCotizacion(codigo);

        // 3. Preparar los datos para la plantilla HTML
        Context context = new Context(Locale.forLanguageTag("es-PE"));

        context.setVariable("cotizacion", cotizacion);
        context.setVariable("detalles", detalles);
        context.setVariable("logoDataUri", logoDataUri);
        context.setVariable("alcanceParrafos", cotizacion.getAlcanceServicio() == null
                ? List.of() : java.util.Arrays.asList(cotizacion.getAlcanceServicio().split("\\R", -1)));

        // 4. Procesar la plantilla HTML con Thymeleaf
        String html = templateEngine.process("cotizacion", context);

        // 5. Convertir el HTML en PDF
        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al generar el PDF de la cotización: " + codigo,
                    e
            );
        }
    }

    private static String cargarLogo() {
        // El recurso se empaqueta en el JAR; el PDF no depende de rutas locales ni de un servidor HTTP.
        try (InputStream logo = new ClassPathResource("static/images/logo-incovec.png").getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(logo.readAllBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar el logo de INCOVEC para las cotizaciones", ex);
        }
    }
}
