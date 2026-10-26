package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.OportunidadComercial;
import com.gestioncomercial.backend.repository.OportunidadComercialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OportunidadComercialService {

    private final OportunidadComercialRepository oportunidadRepository;

    public OportunidadComercialService(
            OportunidadComercialRepository oportunidadRepository) {
        this.oportunidadRepository = oportunidadRepository;
    }

    public List<OportunidadComercial> listarOportunidades() {
        return oportunidadRepository.findAll();
    }

    public OportunidadComercial buscarOportunidadPorId(Long id) {
        return oportunidadRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Oportunidad no encontrada con ID: " + id
                        )
                );
    }

    public OportunidadComercial guardarOportunidad( OportunidadComercial oportunidad) {
        return oportunidadRepository.save(oportunidad);
    }

    public void eliminarOportunidad(Long id) {
        OportunidadComercial oportunidad = buscarOportunidadPorId(id);
        oportunidadRepository.delete(oportunidad);
    }
}
