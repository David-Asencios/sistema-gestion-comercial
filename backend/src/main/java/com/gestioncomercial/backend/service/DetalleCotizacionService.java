package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.DetalleCotizacion;
import com.gestioncomercial.backend.repository.DetalleCotizacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DetalleCotizacionService {

    private final DetalleCotizacionRepository detalleRepository;

    public DetalleCotizacionService(
            DetalleCotizacionRepository detalleRepository) {
        this.detalleRepository = detalleRepository;
    }

    public List<DetalleCotizacion> listarDetalles() {
        return detalleRepository.findAll();
    }

    public DetalleCotizacion buscarDetallePorId(Long id) {
        return detalleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Detalle de cotización no encontrado con ID: " + id
                        )
                );
    }

    public DetalleCotizacion guardarDetalle(DetalleCotizacion detalle) {
        return detalleRepository.save(detalle);
    }

    public DetalleCotizacion actualizarDetalleCotizacion(Long id, DetalleCotizacion detalle){

        DetalleCotizacion detalleExistente = buscarDetallePorId(id);

        detalleExistente.setDescripcion(detalle.getDescripcion());
        detalleExistente.setCantidad(detalle.getCantidad());
        detalleExistente.setPrecioUnitario(detalle.getPrecioUnitario());
        detalleExistente.setSubtotal(detalle.getSubtotal());
        detalleExistente.setCotizacion(detalle.getCotizacion());
        detalleExistente.setServicio(detalle.getServicio());
        detalleExistente.setProducto(detalle.getProducto());

        return detalleRepository.save(detalleExistente);

    }

    public void eliminarDetalle(Long id) {
        DetalleCotizacion detalle = buscarDetallePorId(id);
        detalleRepository.delete(detalle);
    }
}
