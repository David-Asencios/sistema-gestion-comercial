package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.Servicio;

public final class ServicioMapper {
    private ServicioMapper() {}

    public static Servicio toEntity(ServicioRequestDTO request) {
        Servicio concepto = new Servicio();
        concepto.setNombre(request.getNombre());
        concepto.setDescripcion(request.getDescripcion());
        concepto.setPrecioReferencial(request.getPrecioReferencial());
        concepto.setActivo(request.getActivo());
        return concepto;
    }

    public static ServicioResponseDTO toResponse(Servicio concepto) {
        return new ServicioResponseDTO(concepto.getId(), concepto.getNombre(),
                concepto.getDescripcion(), concepto.getPrecioReferencial(), concepto.getActivo());
    }
}
