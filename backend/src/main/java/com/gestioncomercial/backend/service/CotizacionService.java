
package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.entity.EstadoCotizacion;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.CotizacionRepository;
import com.gestioncomercial.backend.repository.DetalleCotizacionRepository;
import com.gestioncomercial.backend.repository.OportunidadComercialRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final OportunidadComercialRepository oportunidadRepository;
    private final DetalleCotizacionRepository detalleCotizacionRepository;

    public CotizacionService(
            CotizacionRepository cotizacionRepository,
            OportunidadComercialRepository oportunidadRepository,
            DetalleCotizacionRepository detalleCotizacionRepository) {

        this.cotizacionRepository = cotizacionRepository;
        this.oportunidadRepository = oportunidadRepository;
        this.detalleCotizacionRepository = detalleCotizacionRepository;
    }

    public List<Cotizacion> listarCotizaciones() {
        return cotizacionRepository.listarConOportunidad();
    }

    public Cotizacion buscarCotizacionPorId(Long id) {
        return cotizacionRepository.buscarPorIdConOportunidad(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cotización no encontrada con ID: " + id));
    }

    @Transactional
    public Cotizacion guardarCotizacion(Cotizacion cotizacion) {

        validarOportunidad(cotizacion);

        cotizacion.setCodigo(generarCodigoCotizacion());

        if (cotizacion.getFechaEmision() == null) {
            cotizacion.setFechaEmision(LocalDate.now());
        }

        cotizacion.setEstado(EstadoCotizacion.BORRADOR);

        cotizacion.setSubtotal(BigDecimal.ZERO);
        cotizacion.setIgv(BigDecimal.ZERO);
        cotizacion.setTotal(BigDecimal.ZERO);

        return cotizacionRepository.save(cotizacion);
    }

    @Transactional
    public Cotizacion actualizarCotizacion(Long id, Cotizacion cotizacion) {

        Cotizacion existente = buscarCotizacionPorId(id);

        if (existente.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new IllegalArgumentException(
                    "Solo se pueden actualizar cotizaciones en estado BORRADOR"
            );
        }

        validarOportunidad(cotizacion);

        existente.setFechaEmision(cotizacion.getFechaEmision());
        existente.setFechaVencimiento(cotizacion.getFechaVencimiento());

        existente.setOportunidad(
                oportunidadRepository.findById(
                                cotizacion.getOportunidad().getId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Oportunidad comercial no encontrada con ID: "
                                        + cotizacion.getOportunidad().getId()))
        );

        return cotizacionRepository.save(existente);
    }

    @Transactional
    public Cotizacion actualizarEstadoCotizacion(
            Long id, EstadoCotizacion nuevoEstado) {

        Cotizacion cotizacion = buscarCotizacionPorId(id);

        if (nuevoEstado == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el nuevo estado de la cotización");
        }

        EstadoCotizacion estadoActual = cotizacion.getEstado();

        boolean transicionPermitida = switch (estadoActual) {
            case BORRADOR -> nuevoEstado == EstadoCotizacion.ENVIADA;

            case ENVIADA ->
                    nuevoEstado == EstadoCotizacion.ACEPTADA
                            || nuevoEstado == EstadoCotizacion.RECHAZADA
                            || nuevoEstado == EstadoCotizacion.VENCIDA;

            case ACEPTADA, RECHAZADA, VENCIDA -> false;
        };

        if (!transicionPermitida) {
            throw new IllegalArgumentException(
                    "No se permite cambiar el estado de "
                            + estadoActual + " a " + nuevoEstado);
        }

        cotizacion.setEstado(nuevoEstado);

        return cotizacionRepository.save(cotizacion);
    }

    @Transactional
    public void eliminarCotizacion(Long id) {
        Cotizacion cotizacion = buscarCotizacionPorId(id);

        if (cotizacion.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new IllegalArgumentException(
                    "Solo se pueden eliminar cotizaciones en estado BORRADOR"
            );
        }

        if (detalleCotizacionRepository.existsByCotizacionId(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar la cotización porque tiene detalles asociados"
            );
        }

        cotizacionRepository.delete(cotizacion);
    }

    private void validarOportunidad(Cotizacion cotizacion) {
        if (cotizacion == null
                || cotizacion.getOportunidad() == null
                || cotizacion.getOportunidad().getId() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una oportunidad comercial");
        }

        Long oportunidadId = cotizacion.getOportunidad().getId();

        if (!oportunidadRepository.existsById(oportunidadId)) {
            throw new RecursoNoEncontradoException(
                    "Oportunidad comercial no encontrada con ID: "
                            + oportunidadId);
        }
    }

    private String generarCodigoCotizacion() {

        int anio = Year.now().getValue();

        String prefijo = "COT-" + anio + "-%";

        String ultimoCodigo = cotizacionRepository
                .obtenerUltimoCodigo(prefijo);

        int siguienteNumero = 1;

        if (ultimoCodigo != null) {
            String[] partes = ultimoCodigo.split("-");
            siguienteNumero = Integer.parseInt(partes[2]) + 1;
        }

        return String.format("COT-%d-%03d", anio, siguienteNumero);
    }
}
