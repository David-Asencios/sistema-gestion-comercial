package com.gestioncomercial.backend.dto;

import jakarta.validation.constraints.*;

/** Campos editables del contrato HTTP; los valores iniciales se aplican si se omiten. */
public class ClienteRequestDTO {
    @Size(max = 11)
    private String ruc;

    @NotBlank @Size(max = 100)
    private String razonSocial;

    @NotBlank @Size(max = 100)
    private String nombreContacto;

    @Size(max = 9)
    private String telefono;

    @Email @Size(max = 100)
    private String correo;

    @Size(max = 250)
    private String direccion;

    @NotNull
    private Boolean activo = true;

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

}
