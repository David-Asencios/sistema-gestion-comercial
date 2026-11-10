package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.DetalleCotizacion.TipoDescuento;
import java.math.BigDecimal;

public record DetalleCotizacionResponseDTO(
        Long id, String descripcion, BigDecimal cantidad, BigDecimal precioUnitario,
        TipoDescuento tipoDescuento, BigDecimal valorDescuento,
        BigDecimal descuentoUnitario, BigDecimal descuentoTotal, BigDecimal subtotal,
        CotizacionResponseDTO cotizacion, ConceptoDTO servicio, ConceptoDTO producto) {

    public record ConceptoDTO(
            Long id, String nombre, String descripcion,
            BigDecimal precioReferencial, Boolean activo) {
    }
}
