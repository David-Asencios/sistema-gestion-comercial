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

    public void eliminarDetalle(Long id) {
        DetalleCotizacion detalle = buscarDetallePorId(id);
        detalleRepository.delete(detalle);
    }
}
