
package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cliente;
import com.gestioncomercial.backend.entity.OportunidadComercial;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.ClienteRepository;
import com.gestioncomercial.backend.repository.OportunidadComercialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OportunidadComercialService {

    private final OportunidadComercialRepository oportunidadRepository;
    private final ClienteRepository clienteRepository;

    public OportunidadComercialService(
            OportunidadComercialRepository oportunidadRepository,
            ClienteRepository clienteRepository) {
        this.oportunidadRepository = oportunidadRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<OportunidadComercial> listarOportunidades() {
        return oportunidadRepository.listarConCliente();
    }

    public OportunidadComercial buscarOportunidadPorId(Long id) {
        return oportunidadRepository.buscarPorIdConCliente(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Oportunidad no encontrada con ID: " + id
                        )
                );
    }

    public OportunidadComercial guardarOportunidad(
            OportunidadComercial oportunidad) {

        validarCliente(oportunidad.getCliente().getId());
        return oportunidadRepository.save(oportunidad);
    }

    public OportunidadComercial actualizarOportunidadComercial(
            Long id, OportunidadComercial oportunidad) {

        OportunidadComercial oportunidadExistente = buscarOportunidadPorId(id);

        validarCliente(oportunidad.getCliente().getId());

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

    private void validarCliente(Long clienteId) {

        if (clienteId == null || !clienteRepository.existsById(clienteId)) {

            throw new RecursoNoEncontradoException("Cliente no encontrado con ID: " + clienteId);
        }
    }
}
