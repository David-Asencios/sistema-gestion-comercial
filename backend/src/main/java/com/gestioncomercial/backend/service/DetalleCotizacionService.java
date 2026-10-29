
package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.entity.DetalleCotizacion;
import com.gestioncomercial.backend.entity.EstadoCotizacion;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.CotizacionRepository;
import com.gestioncomercial.backend.repository.DetalleCotizacionRepository;
import com.gestioncomercial.backend.repository.ProductoRepository;
import com.gestioncomercial.backend.repository.ServicioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DetalleCotizacionService {

    private final DetalleCotizacionRepository detalleRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;

    public DetalleCotizacionService(
            DetalleCotizacionRepository detalleRepository,
            CotizacionRepository cotizacionRepository,
            ServicioRepository servicioRepository,
            ProductoRepository productoRepository) {
        this.detalleRepository = detalleRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.servicioRepository = servicioRepository;
        this.productoRepository = productoRepository;
    }

    public List<DetalleCotizacion> listarDetalles() {
        return detalleRepository.listarConRelaciones();
    }

    public DetalleCotizacion buscarDetallePorId(Long id) {
        return detalleRepository.buscarPorIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Detalle de cotización no encontrado con ID: " + id));
    }

    @Transactional
    public DetalleCotizacion guardarDetalle(DetalleCotizacion detalle) {
        validarDetalle(detalle);

        Cotizacion cotizacion = obtenerCotizacion(detalle);
        validarCotizacionEditable(cotizacion);
        asignarRelaciones(detalle, cotizacion);

        calcularSubtotal(detalle);

        DetalleCotizacion detalleGuardado = detalleRepository.save(detalle);
        actualizarTotales(cotizacion);

        return detalleRepository.buscarPorIdConRelaciones(
                detalleGuardado.getId()
        ).orElseThrow(() -> new RecursoNoEncontradoException(
                "Detalle de cotización no encontrado después de guardarlo"));
    }

    @Transactional
    public DetalleCotizacion actualizarDetalleCotizacion(
            Long id, DetalleCotizacion detalle) {

        DetalleCotizacion detalleExistente = buscarDetallePorId(id);
        validarDetalle(detalle);

        Long cotizacionAnteriorId = detalleExistente.getCotizacion().getId();

        Cotizacion nuevaCotizacion = obtenerCotizacion(detalle);
        validarCotizacionEditable(nuevaCotizacion);

        detalleExistente.setDescripcion(detalle.getDescripcion());
        detalleExistente.setCantidad(detalle.getCantidad());
        detalleExistente.setPrecioUnitario(detalle.getPrecioUnitario());
        detalleExistente.setCotizacion(nuevaCotizacion);

        asignarRelaciones(detalleExistente, nuevaCotizacion, detalle);
        calcularSubtotal(detalleExistente);

        DetalleCotizacion detalleActualizado =
                detalleRepository.save(detalleExistente);

        actualizarTotales(nuevaCotizacion);

        if (!cotizacionAnteriorId.equals(nuevaCotizacion.getId())) {
            Cotizacion cotizacionAnterior =
                    cotizacionRepository.findById(cotizacionAnteriorId)
                            .orElseThrow(() -> new RecursoNoEncontradoException(
                                    "Cotización no encontrada con ID: "
                                            + cotizacionAnteriorId));

            actualizarTotales(cotizacionAnterior);
        }

        return detalleActualizado;
    }

    @Transactional
    public void eliminarDetalle(Long id) {
        DetalleCotizacion detalle = buscarDetallePorId(id);

        Long cotizacionId = detalle.getCotizacion().getId();

        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cotización no encontrada con ID: " + cotizacionId));

        validarCotizacionEditable(cotizacion);

        detalleRepository.delete(detalle);

        actualizarTotales(cotizacion);
    }

    private void validarDetalle(DetalleCotizacion detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El detalle de cotización es obligatorio");
        }

        if (detalle.getCantidad() == null
                || detalle.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero");
        }

        if (detalle.getPrecioUnitario() == null
                || detalle.getPrecioUnitario()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El precio unitario no puede ser negativo");
        }

        boolean tieneServicio = detalle.getServicio() != null;
        boolean tieneProducto = detalle.getProducto() != null;

        if (tieneServicio == tieneProducto) {
            throw new IllegalArgumentException(
                    "El detalle debe tener un servicio o un producto, pero no ambos");
        }

        if (tieneServicio) {
            Long servicioId = detalle.getServicio().getId();

            if (servicioId == null
                    || !servicioRepository.existsById(servicioId)) {
                throw new RecursoNoEncontradoException(
                        "El servicio indicado no existe");
            }
        }

        if (tieneProducto) {
            Long productoId = detalle.getProducto().getId();

            if (productoId == null
                    || !productoRepository.existsById(productoId)) {
                throw new RecursoNoEncontradoException(
                        "El producto indicado no existe");
            }
        }
    }

    private Cotizacion obtenerCotizacion(DetalleCotizacion detalle) {
        if (detalle.getCotizacion() == null
                || detalle.getCotizacion().getId() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una cotización");
        }

        Long cotizacionId = detalle.getCotizacion().getId();

        return cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cotización no encontrada con ID: " + cotizacionId));
    }

    private void asignarRelaciones(
            DetalleCotizacion detalle, Cotizacion cotizacion) {

        asignarRelaciones(detalle, cotizacion, detalle);
    }

    private void asignarRelaciones(
            DetalleCotizacion destino,
            Cotizacion cotizacion,
            DetalleCotizacion origen) {

        destino.setCotizacion(cotizacion);

        if (origen.getServicio() != null) {
            destino.setServicio(
                    servicioRepository.getReferenceById(
                            origen.getServicio().getId()));
            destino.setProducto(null);
        } else {
            destino.setProducto(
                    productoRepository.getReferenceById(
                            origen.getProducto().getId()));
            destino.setServicio(null);
        }
    }

    private void calcularSubtotal(DetalleCotizacion detalle) {
        BigDecimal subtotal = detalle.getCantidad()
                .multiply(detalle.getPrecioUnitario())
                .setScale(2, RoundingMode.HALF_UP);

        detalle.setSubtotal(subtotal);
    }

    private void actualizarTotales(Cotizacion cotizacion) {
        BigDecimal subtotal = detalleRepository
                .sumarSubtotalPorCotizacion(cotizacion.getId())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = subtotal.add(igv)
                .setScale(2, RoundingMode.HALF_UP);

        cotizacion.setSubtotal(subtotal);
        cotizacion.setIgv(igv);
        cotizacion.setTotal(total);

        cotizacionRepository.save(cotizacion);
    }

    private void validarCotizacionEditable(Cotizacion cotizacion) {
        if (cotizacion.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new IllegalArgumentException(
                    "Solo se pueden modificar los detalles de cotizaciones en estado BORRADOR"
            );
        }
    }

}
