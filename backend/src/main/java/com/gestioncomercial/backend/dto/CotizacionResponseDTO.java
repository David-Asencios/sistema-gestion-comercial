package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.CanalOrigen;
import com.gestioncomercial.backend.entity.EstadoCotizacion;
import com.gestioncomercial.backend.entity.EtapaOportunidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CotizacionResponseDTO(
        Long id, String codigo, LocalDate fechaEmision, LocalDate fechaVencimiento,
        Integer vigenciaDias, String alcanceServicio, EstadoCotizacion estado,
        BigDecimal subtotal, BigDecimal igv, BigDecimal total,
        OportunidadDTO oportunidad) {

    public record OportunidadDTO(
            Long id, String titulo, String descripcion, CanalOrigen canalOrigen,
            EtapaOportunidad etapa, LocalDateTime fechaCreacion, String observaciones,
            ClienteDTO cliente) {
    }

    public record ClienteDTO(
            Long id, String ruc, String razonSocial, String nombreContacto,
            String telefono, String correo, String direccion, Boolean activo) {
    }
}
