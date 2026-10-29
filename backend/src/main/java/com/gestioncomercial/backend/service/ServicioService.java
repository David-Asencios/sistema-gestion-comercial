package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Servicio;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.ServicioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public ServicioService(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    public List<Servicio> listarServicios() {
        return servicioRepository.findAll();
    }

    public Servicio buscarServicioPorId(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Servicio no encontrado con ID: " + id
                        )
                );
    }

    public Servicio guardarServicio(Servicio servicio) {
        return servicioRepository.save(servicio);
    }

    public void eliminarServicio(Long id) {
        Servicio servicio = buscarServicioPorId(id);
        servicioRepository.delete(servicio);
    }

    public Servicio actualizarServicio(Long id, Servicio servicio){

        Servicio servicioExistente = buscarServicioPorId(id);

        servicioExistente.setNombre(servicio.getNombre());
        servicioExistente.setDescripcion(servicio.getDescripcion());
        servicioExistente.setPrecioReferencial( servicio.getPrecioReferencial());
        servicioExistente.setActivo(servicio.getActivo());

        return servicioRepository.save(servicioExistente);
    }


}

