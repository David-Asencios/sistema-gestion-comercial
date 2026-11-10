
package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.entity.EstadoCotizacion;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.CotizacionRepository;
import com.gestioncomercial.backend.repository.DetalleCotizacionRepository;
import com.gestioncomercial.backend.repository.OportunidadComercialRepository;
import com.gestioncomercial.backend.repository.SecuenciaCotizacionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final OportunidadComercialRepository oportunidadRepository;
    private final DetalleCotizacionRepository detalleCotizacionRepository;
    private final SecuenciaCotizacionRepository secuenciaRepository;

    public CotizacionService(
            CotizacionRepository cotizacionRepository,
            OportunidadComercialRepository oportunidadRepository,
            DetalleCotizacionRepository detalleCotizacionRepository,
            SecuenciaCotizacionRepository secuenciaRepository) {

        this.cotizacionRepository = cotizacionRepository;
        this.oportunidadRepository = oportunidadRepository;
        this.detalleCotizacionRepository = detalleCotizacionRepository;
        this.secuenciaRepository = secuenciaRepository;
    }

    public List<Cotizacion> listarCotizaciones() {
        return cotizacionRepository.listarConOportunidad();
    }

    public Page<Cotizacion> buscarCotizaciones(String codigo, Long clienteId, Long oportunidadId,
            EstadoCotizacion estado, LocalDate fechaDesde, LocalDate fechaHasta,
            int pagina, int tamano, String orden, String direccion) {
        if (clienteId != null && clienteId <= 0 || oportunidadId != null && oportunidadId <= 0) {
            throw new IllegalArgumentException("Los IDs de cliente y oportunidad deben ser positivos");
        }
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        var solicitud = ConsultaPaginada.pagina(pagina, tamano, orden, direccion,
                Set.of("id", "codigo", "fechaEmision", "fechaVencimiento", "estado", "subtotal", "igv", "total"));
        return cotizacionRepository.buscar(ConsultaPaginada.patron(codigo), clienteId, oportunidadId,
                estado, fechaDesde, fechaHasta, solicitud);
    }

    public Cotizacion buscarCotizacionPorId(Long id) {
        return cotizacionRepository.buscarPorIdConOportunidad(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cotización no encontrada con ID: " + id));
    }

    @Transactional
    public Cotizacion guardarCotizacion(Cotizacion cotizacion) {

        validarOportunidad(cotizacion);

        if (cotizacion.getFechaEmision() == null) {
            cotizacion.setFechaEmision(LocalDate.now());
        }

        if (cotizacion.getVigenciaDias() == null) {
            cotizacion.setVigenciaDias(15);
        }

        if (cotizacion.getVigenciaDias() <= 0) {
            throw new IllegalArgumentException(
                    "La vigencia debe ser mayor a cero días");
        }

        cotizacion.setFechaVencimiento(
                cotizacion.getFechaEmision()
                        .plusDays(cotizacion.getVigenciaDias())
        );

        cotizacion.setEstado(EstadoCotizacion.BORRADOR);

        cotizacion.setSubtotal(BigDecimal.ZERO);
        cotizacion.setIgv(BigDecimal.ZERO);
        cotizacion.setTotal(BigDecimal.ZERO);

        cotizacion.setCodigo(generarCodigoCotizacion());
        return cotizacionRepository.save(cotizacion);
    }

    @Transactional
    public Cotizacion actualizarCotizacion(Long id, Cotizacion cotizacion) {

        Cotizacion existente = bloquearCotizacion(id);

        if (existente.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new IllegalArgumentException(
                    "Solo se pueden actualizar cotizaciones en estado BORRADOR"
            );
        }

        validarOportunidad(cotizacion);

        // Actualizar fecha de emisión
        if (cotizacion.getFechaEmision() != null) {
            existente.setFechaEmision(cotizacion.getFechaEmision());
        }

        // Actualizar vigencia
        Integer vigencia = cotizacion.getVigenciaDias() != null
                ? cotizacion.getVigenciaDias()
                : existente.getVigenciaDias();

        if (vigencia <= 0) {
            throw new IllegalArgumentException(
                    "La vigencia debe ser mayor a cero días"
            );
        }

        existente.setVigenciaDias(vigencia);

        // Calcular nuevamente la fecha de vencimiento
        existente.setFechaVencimiento(
                existente.getFechaEmision().plusDays(vigencia)
        );

        // Actualizar alcances y consideraciones
        existente.setAlcanceServicio(cotizacion.getAlcanceServicio());

        // Actualizar oportunidad comercial
        existente.setOportunidad(cotizacion.getOportunidad());

        cotizacionRepository.save(existente);
        return buscarCotizacionPorId(id);
    }

    @Transactional
    public Cotizacion actualizarEstadoCotizacion(
            Long id, EstadoCotizacion nuevoEstado) {

        Cotizacion cotizacion = bloquearCotizacion(id);

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

        validarCondicionesDelEstado(cotizacion, nuevoEstado);
        cotizacion.setEstado(nuevoEstado);

        cotizacionRepository.save(cotizacion);
        return buscarCotizacionPorId(id);
    }

    private void validarCondicionesDelEstado(Cotizacion cotizacion, EstadoCotizacion nuevoEstado) {
        if (nuevoEstado == EstadoCotizacion.ENVIADA
                && !detalleCotizacionRepository.existsByCotizacionId(cotizacion.getId())) {
            throw new IllegalArgumentException("No se puede enviar una cotización sin detalles");
        }

        if (nuevoEstado == EstadoCotizacion.ENVIADA
                || nuevoEstado == EstadoCotizacion.ACEPTADA
                || nuevoEstado == EstadoCotizacion.VENCIDA) {
            LocalDate vencimiento = cotizacion.getFechaVencimiento();
            if (vencimiento == null) {
                throw new IllegalArgumentException("La cotización debe tener una fecha de vencimiento");
            }
            // La cotización conserva su vigencia durante toda la fecha de vencimiento.
            boolean vencida = vencimiento.isBefore(LocalDate.now());
            if (nuevoEstado == EstadoCotizacion.VENCIDA && !vencida) {
                throw new IllegalArgumentException("La cotización todavía está vigente");
            }
            if (nuevoEstado != EstadoCotizacion.VENCIDA && vencida) {
                throw new IllegalArgumentException("No se puede enviar ni aceptar una cotización vencida");
            }
        }
    }

    @Transactional
    public void eliminarCotizacion(Long id) {
        Cotizacion cotizacion = bloquearCotizacion(id);

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

        cotizacion.setOportunidad(oportunidadRepository.buscarPorIdConCliente(oportunidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Oportunidad comercial no encontrada con ID: " + oportunidadId)));
    }

    private String generarCodigoCotizacion() {
        var secuencia = secuenciaRepository.obtenerParaActualizar().orElseThrow(() ->
                new IllegalStateException("Debe inicializar la secuencia de cotizaciones con el script 002"));
        return String.format(Locale.ROOT, "COT-%05d", secuencia.siguienteNumero());
    }

    private Cotizacion bloquearCotizacion(Long id) {
        return cotizacionRepository.buscarPorIdParaActualizar(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Cotización no encontrada con ID: " + id));
    }

    public Cotizacion buscarPorCodigo(String codigo) {
        return cotizacionRepository.buscarPorCodigoConOportunidad(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cotización no encontrada con código: " + codigo
                ));
    }

}
