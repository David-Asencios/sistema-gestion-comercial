package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.DetalleCotizacion.TipoDescuento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DetalleCotizacionRequestDTO(
        @NotNull @Valid ReferenciaDTO cotizacion,
        @Valid ReferenciaDTO servicio,
        @Valid ReferenciaDTO producto,
        @NotBlank @Size(max = 200) String descripcion,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal cantidad,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precioUnitario,
        TipoDescuento tipoDescuento,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal valorDescuento) {
}
