package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.Cliente;
import com.gestioncomercial.backend.entity.OportunidadComercial;

public final class OportunidadComercialMapper {
    private OportunidadComercialMapper() {}

    public static OportunidadComercial toEntity(OportunidadComercialRequestDTO request) {
        OportunidadComercial oportunidad = new OportunidadComercial();
        oportunidad.setTitulo(request.getTitulo());
        oportunidad.setDescripcion(request.getDescripcion());
        oportunidad.setCanalOrigen(request.getCanalOrigen());
        oportunidad.setEtapa(request.getEtapa());
        oportunidad.setObservaciones(request.getObservaciones());
        Cliente cliente = new Cliente();
        cliente.setId(request.getCliente().id());
        oportunidad.setCliente(cliente);
        return oportunidad;
    }

    public static OportunidadComercialResponseDTO toResponse(OportunidadComercial oportunidad) {
        return new OportunidadComercialResponseDTO(
                oportunidad.getId(), oportunidad.getTitulo(), oportunidad.getDescripcion(),
                oportunidad.getCanalOrigen(), oportunidad.getEtapa(), oportunidad.getFechaCreacion(),
                oportunidad.getObservaciones(), ClienteMapper.toResponse(oportunidad.getCliente()));
    }
}
