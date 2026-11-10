package com.gestioncomercial.backend.dto;

public record ClienteResponseDTO(
        Long id, String ruc, String razonSocial, String nombreContacto,
        String telefono, String correo, String direccion, Boolean activo) {
}
