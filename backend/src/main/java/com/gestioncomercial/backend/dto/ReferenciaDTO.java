package com.gestioncomercial.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReferenciaDTO(@NotNull @Positive Long id) {
}
