package com.gestioncomercial.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Campos editables del catálogo; el ID lo asigna el servidor. */
public class ServicioRequestDTO {
    @NotBlank
    @Size(max = 150)
    private String nombre;

    private String descripcion;

    @NotNull(message = "El precio referencial es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta ocho enteros y dos decimales")
    private BigDecimal precioReferencial;

    @NotNull
    private Boolean activo = true;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecioReferencial() { return precioReferencial; }
    public void setPrecioReferencial(BigDecimal precioReferencial) { this.precioReferencial = precioReferencial; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
