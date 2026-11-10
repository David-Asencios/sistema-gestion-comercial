package com.gestioncomercial.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record CotizacionRequestDTO(
        @NotNull @Valid ReferenciaDTO oportunidad,
        LocalDate fechaEmision,
        @Positive Integer vigenciaDias,
        String alcanceServicio) {
}
