
package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cliente;
import com.gestioncomercial.backend.entity.CanalOrigen;
import com.gestioncomercial.backend.entity.EtapaOportunidad;
import com.gestioncomercial.backend.entity.OportunidadComercial;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.ClienteRepository;
import com.gestioncomercial.backend.repository.OportunidadComercialRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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

    public Page<OportunidadComercial> buscarOportunidades(String texto, Long clienteId,
            EtapaOportunidad etapa, CanalOrigen canalOrigen, int pagina, int tamano,
            String orden, String direccion) {
        if (clienteId != null && clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser positivo");
        }
        var solicitud = ConsultaPaginada.pagina(pagina, tamano, orden, direccion,
                Set.of("id", "titulo", "fechaCreacion", "etapa", "canalOrigen"));
        return oportunidadRepository.buscar(ConsultaPaginada.patron(texto), clienteId, etapa, canalOrigen, solicitud);
    }

    public OportunidadComercial buscarOportunidadPorId(Long id) {
        return oportunidadRepository.buscarPorIdConCliente(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Oportunidad no encontrada con ID: " + id
                        )
                );
    }

    @Transactional
    public OportunidadComercial guardarOportunidad(
            OportunidadComercial oportunidad) {

        oportunidad.setCliente(obtenerCliente(oportunidad));
        return oportunidadRepository.save(oportunidad);
    }

    @Transactional
    public OportunidadComercial actualizarOportunidadComercial(
            Long id, OportunidadComercial oportunidad) {

        OportunidadComercial oportunidadExistente = buscarOportunidadPorId(id);

        Cliente cliente = obtenerCliente(oportunidad);

        oportunidadExistente.setTitulo(oportunidad.getTitulo());
        oportunidadExistente.setDescripcion(oportunidad.getDescripcion());
        oportunidadExistente.setCanalOrigen(oportunidad.getCanalOrigen());
        oportunidadExistente.setEtapa(oportunidad.getEtapa());
        oportunidadExistente.setObservaciones(oportunidad.getObservaciones());
        oportunidadExistente.setCliente(cliente);

        return oportunidadRepository.save(oportunidadExistente);
    }

    public void eliminarOportunidad(Long id) {
        OportunidadComercial oportunidad = buscarOportunidadPorId(id);
        oportunidadRepository.delete(oportunidad);
    }

    private Cliente obtenerCliente(OportunidadComercial oportunidad) {
        if (oportunidad == null || oportunidad.getCliente() == null
                || oportunidad.getCliente().getId() == null
                || oportunidad.getCliente().getId() <= 0) {
            throw new IllegalArgumentException("Debe indicar un cliente con un ID válido");
        }
        Long clienteId = oportunidad.getCliente().getId();
        return clienteRepository.findById(clienteId).orElseThrow(() ->
                new RecursoNoEncontradoException("Cliente no encontrado con ID: " + clienteId));
    }
}
