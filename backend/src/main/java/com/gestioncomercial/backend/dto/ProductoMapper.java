package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.Producto;

public final class ProductoMapper {
    private ProductoMapper() {}

    public static Producto toEntity(ProductoRequestDTO request) {
        Producto concepto = new Producto();
        concepto.setNombre(request.getNombre());
        concepto.setDescripcion(request.getDescripcion());
        concepto.setPrecioReferencial(request.getPrecioReferencial());
        concepto.setActivo(request.getActivo());
        return concepto;
    }

    public static ProductoResponseDTO toResponse(Producto concepto) {
        return new ProductoResponseDTO(concepto.getId(), concepto.getNombre(),
                concepto.getDescripcion(), concepto.getPrecioReferencial(), concepto.getActivo());
    }
}
