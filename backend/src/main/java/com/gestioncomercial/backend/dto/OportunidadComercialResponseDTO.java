package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.CanalOrigen;
import com.gestioncomercial.backend.entity.EtapaOportunidad;
import java.time.LocalDateTime;

public record OportunidadComercialResponseDTO(
        Long id, String titulo, String descripcion, CanalOrigen canalOrigen,
        EtapaOportunidad etapa, LocalDateTime fechaCreacion, String observaciones,
        ClienteResponseDTO cliente) {
}
