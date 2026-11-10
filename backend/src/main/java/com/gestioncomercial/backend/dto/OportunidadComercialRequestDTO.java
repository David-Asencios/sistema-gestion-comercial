package com.gestioncomercial.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.gestioncomercial.backend.entity.CanalOrigen;
import com.gestioncomercial.backend.entity.EtapaOportunidad;

/** Campos editables del contrato HTTP; los valores iniciales se aplican si se omiten. */
public class OportunidadComercialRequestDTO {
    @NotBlank(message = "El título de la oportunidad es obligatorio") @Size(max = 150)
    private String titulo;

    private String descripcion;

    @NotNull
    private CanalOrigen canalOrigen;

    @NotNull
    private EtapaOportunidad etapa = EtapaOportunidad.NUEVA;

    private String observaciones;

    @NotNull @Valid
    private ReferenciaDTO cliente;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public void setCanalOrigen(CanalOrigen canalOrigen) { this.canalOrigen = canalOrigen; }

    public EtapaOportunidad getEtapa() { return etapa; }
    public void setEtapa(EtapaOportunidad etapa) { this.etapa = etapa; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public ReferenciaDTO getCliente() { return cliente; }
    public void setCliente(ReferenciaDTO cliente) { this.cliente = cliente; }

}
