package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.repository.CotizacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;

    public CotizacionService(CotizacionRepository cotizacionRepository) {
        this.cotizacionRepository = cotizacionRepository;
    }

    public List<Cotizacion> listarCotizaciones() {
        return cotizacionRepository.findAll();
    }

    public Cotizacion buscarCotizacionPorId(Long id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cotización no encontrada con ID: " + id
                        )
                );
    }

    public Cotizacion guardarCotizacion(Cotizacion cotizacion) {
        return cotizacionRepository.save(cotizacion);
    }

    public void eliminarCotizacion(Long id) {
        Cotizacion cotizacion = buscarCotizacionPorId(id);
        cotizacionRepository.delete(cotizacion);
    }
}
