
package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.entity.DetalleCotizacion;
import com.gestioncomercial.backend.entity.EstadoCotizacion;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.exception.ConflictoConcurrenteException;
import com.gestioncomercial.backend.repository.CotizacionRepository;
import com.gestioncomercial.backend.repository.DetalleCotizacionRepository;
import com.gestioncomercial.backend.repository.ProductoRepository;
import com.gestioncomercial.backend.repository.ServicioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DetalleCotizacion guardarDetalle(DetalleCotizacion detalle) {
        validarDetalle(detalle);

        Cotizacion cotizacion = bloquearCotizacion(obtenerCotizacionId(detalle));
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


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DetalleCotizacion actualizarDetalleCotizacion(
            Long id, DetalleCotizacion detalle) {

        validarDetalle(detalle);

        Long cotizacionAnteriorId = buscarCotizacionIdDelDetalle(id);
        Long nuevaCotizacionId = obtenerCotizacionId(detalle);
        Map<Long, Cotizacion> bloqueadas = bloquearCotizaciones(cotizacionAnteriorId, nuevaCotizacionId);
        // Leer el detalle después del bloqueo evita reutilizar una fotografía anterior a otra transacción.
        DetalleCotizacion detalleExistente = buscarDetallePorId(id);
        validarCotizacionDelDetalle(detalleExistente, cotizacionAnteriorId);
        Cotizacion cotizacionAnterior = bloqueadas.get(cotizacionAnteriorId);
        validarCotizacionEditable(cotizacionAnterior);

        Cotizacion nuevaCotizacion = bloqueadas.get(nuevaCotizacionId);
        validarCotizacionEditable(nuevaCotizacion);

        detalleExistente.setDescripcion(detalle.getDescripcion());
        detalleExistente.setCantidad(detalle.getCantidad());
        detalleExistente.setPrecioUnitario(detalle.getPrecioUnitario());
        detalleExistente.setTipoDescuento(detalle.getTipoDescuento());
        detalleExistente.setValorDescuento(detalle.getValorDescuento());
        detalleExistente.setCotizacion(nuevaCotizacion);

        asignarRelaciones(detalleExistente, nuevaCotizacion, detalle);
        calcularSubtotal(detalleExistente);

        DetalleCotizacion detalleActualizado =
                detalleRepository.save(detalleExistente);

        if (!cotizacionAnteriorId.equals(nuevaCotizacion.getId())) {
            actualizarTotales(cotizacionAnterior);
        }

        actualizarTotales(nuevaCotizacion);

        return detalleRepository.buscarPorIdConRelaciones(
                detalleActualizado.getId()
        ).orElseThrow(() -> new RecursoNoEncontradoException(
                "Detalle de cotización no encontrado después de actualizarlo"));
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void eliminarDetalle(Long id) {
        Long cotizacionId = buscarCotizacionIdDelDetalle(id);
        Cotizacion cotizacion = bloquearCotizacion(cotizacionId);
        DetalleCotizacion detalle = buscarDetallePorId(id);
        validarCotizacionDelDetalle(detalle, cotizacionId);

        validarCotizacionEditable(cotizacion);

        detalleRepository.delete(detalle);

        actualizarTotales(cotizacion);
    }

    private void validarDetalle(DetalleCotizacion detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El detalle de cotización es obligatorio");
        }

        if (detalle.getDescripcion() == null || detalle.getDescripcion().isBlank()
                || detalle.getDescripcion().length() > 200) {
            throw new IllegalArgumentException("La descripción es obligatoria y admite hasta 200 caracteres");
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

        validarDecimal(detalle.getCantidad(), 8, "cantidad");
        validarDecimal(detalle.getPrecioUnitario(), 10, "precio unitario");

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

    private Long obtenerCotizacionId(DetalleCotizacion detalle) {
        if (detalle.getCotizacion() == null
                || detalle.getCotizacion().getId() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una cotización");
        }

        return detalle.getCotizacion().getId();
    }

    private Long buscarCotizacionIdDelDetalle(Long id) {
        return detalleRepository.buscarCotizacionId(id).orElseThrow(() ->
                new RecursoNoEncontradoException("Detalle de cotización no encontrado con ID: " + id));
    }

    private Cotizacion bloquearCotizacion(Long cotizacionId) {
        return cotizacionRepository.buscarPorIdParaActualizar(cotizacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cotización no encontrada con ID: " + cotizacionId));
    }

    private Map<Long, Cotizacion> bloquearCotizaciones(Long anteriorId, Long nuevaId) {
        Map<Long, Cotizacion> bloqueadas = new TreeMap<>();
        for (Long id : new TreeSet<>(List.of(anteriorId, nuevaId))) {
            bloqueadas.put(id, bloquearCotizacion(id));
        }
        return bloqueadas;
    }

    private void validarCotizacionDelDetalle(DetalleCotizacion detalle, Long cotizacionEsperada) {
        if (!detalle.getCotizacion().getId().equals(cotizacionEsperada)) {
            throw new ConflictoConcurrenteException("El detalle cambió de cotización durante la operación");
        }
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
        var tipo = detalle.getTipoDescuento() == null
                ? DetalleCotizacion.TipoDescuento.NINGUNO : detalle.getTipoDescuento();
        BigDecimal valor = detalle.getValorDescuento();
        if (valor == null) {
            if (tipo != DetalleCotizacion.TipoDescuento.NINGUNO) {
                throw new IllegalArgumentException("Debe indicar el valor del descuento");
            }
            valor = BigDecimal.ZERO;
        }
        validarDecimal(valor, 10, "valor del descuento");
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo");
        }
        BigDecimal precio = detalle.getPrecioUnitario().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal descuento = switch (tipo) {
            case NINGUNO -> {
                if (valor.signum() != 0) {
                    throw new IllegalArgumentException("Sin descuento, el valor debe ser cero");
                }
                yield BigDecimal.ZERO;
            }
            case MONTO -> {
                if (valor.compareTo(precio) > 0) {
                    throw new IllegalArgumentException("El descuento por unidad no puede superar el precio unitario");
                }
                yield valor;
            }
            case PORCENTAJE -> {
                if (valor.compareTo(new BigDecimal("100")) > 0) {
                    throw new IllegalArgumentException("El porcentaje de descuento no puede superar 100");
                }
                yield precio.multiply(valor).movePointLeft(2);
            }
        };
        // Redondear primero por unidad; la línea se obtiene del bruto menos el descuento total.
        descuento = descuento.setScale(2, RoundingMode.HALF_UP);
        BigDecimal bruto = precio.multiply(detalle.getCantidad()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal descuentoTotal = descuento.multiply(detalle.getCantidad()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = bruto.subtract(descuentoTotal);
        validarDecimal(descuentoTotal, 10, "descuento total");
        validarDecimal(subtotal, 10, "subtotal");
        detalle.setTipoDescuento(tipo);
        detalle.setValorDescuento(valor.setScale(2, RoundingMode.UNNECESSARY));
        detalle.setPrecioUnitario(precio);
        detalle.setDescuentoUnitario(descuento);
        detalle.setDescuentoTotal(descuentoTotal);
        detalle.setSubtotal(subtotal);
    }

    private void validarDecimal(BigDecimal valor, int enteros, String campo) {
        try {
            BigDecimal normalizado = valor.setScale(2, RoundingMode.UNNECESSARY);
            if (normalizado.precision() > enteros + 2) {
                throw new IllegalArgumentException("El campo " + campo + " supera el importe permitido");
            }
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("El campo " + campo + " admite como máximo dos decimales");
        }
    }

    private void actualizarTotales(Cotizacion cotizacion) {
        BigDecimal subtotal = detalleRepository
                .sumarSubtotalPorCotizacion(cotizacion.getId())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = subtotal.add(igv)
                .setScale(2, RoundingMode.HALF_UP);

        validarDecimal(subtotal, 10, "subtotal de cotización");
        validarDecimal(igv, 10, "IGV");
        validarDecimal(total, 10, "total de cotización");

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

    public List<DetalleCotizacion> listarPorCodigoCotizacion(String codigo) {

        cotizacionRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cotización no encontrada con código: " + codigo
                ));

        return detalleRepository.listarPorCodigoCotizacion(codigo);
    }

}
