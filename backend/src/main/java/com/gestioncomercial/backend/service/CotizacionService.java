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

    public Cotizacion actualizarCotizacion(Long id, Cotizacion cotizacion){

        Cotizacion cotizacionExistente = buscarCotizacionPorId(id);

        cotizacionExistente.setCodigo(cotizacion.getCodigo());
        cotizacionExistente.setFechaEmision(cotizacion.getFechaEmision());
        cotizacionExistente.setFechaVencimiento(cotizacion.getFechaVencimiento());
        cotizacionExistente.setEstado(cotizacion.getEstado());
        cotizacionExistente.setSubtotal(cotizacion.getSubtotal());
        cotizacionExistente.setIgv(cotizacion.getIgv());
        cotizacionExistente.setTotal(cotizacion.getTotal());
        cotizacionExistente.setOportunidad(cotizacion.getOportunidad());

        return cotizacionRepository.save(cotizacionExistente);

    }

    public void eliminarCotizacion(Long id) {
        Cotizacion cotizacion = buscarCotizacionPorId(id);
        cotizacionRepository.delete(cotizacion);
    }
}
