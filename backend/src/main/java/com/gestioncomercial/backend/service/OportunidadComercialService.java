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

    public OportunidadComercial actualizarOportunidadComercial(Long id, OportunidadComercial oportunidad){

        OportunidadComercial oportunidadExistente = buscarOportunidadPorId(id);

        oportunidadExistente.setTitulo(oportunidad.getTitulo());
        oportunidadExistente.setDescripcion(oportunidad.getDescripcion());
        oportunidadExistente.setCanalOrigen(oportunidad.getCanalOrigen());
        oportunidadExistente.setEtapa(oportunidad.getEtapa());
        oportunidadExistente.setObservaciones(oportunidad.getObservaciones());
        oportunidadExistente.setCliente(oportunidad.getCliente());

        return oportunidadRepository.save(oportunidadExistente);
    }

    public void eliminarOportunidad(Long id) {
        OportunidadComercial oportunidad = buscarOportunidadPorId(id);
        oportunidadRepository.delete(oportunidad);
    }
}
