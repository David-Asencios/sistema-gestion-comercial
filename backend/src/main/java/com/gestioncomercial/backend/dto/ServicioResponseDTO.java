package com.gestioncomercial.backend.dto;

import java.math.BigDecimal;

public record ServicioResponseDTO(
        Long id, String nombre, String descripcion, BigDecimal precioReferencial, Boolean activo) {
}
