package com.gestioncomercial.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.security.PrivateKey;

@Entity
@Table(name="clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max=11)
    @Column(length = 11, unique = true)
    private String ruc;

    @NotBlank
    @Size(max = 100)
    @Column(name= "razon_social", nullable = false, length = 100)
    private String razonSocial;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre_contacto", length = 100)
    private String nombreContacto;

    @Size(max = 9)
    @Column(length = 9)
    private String telefono;

    @Email
    @Size(max = 100)
    @Column(length = 100)
    private String correo;

    @Size(max = 250)
    @Column(length = 250)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo = true;

    public Cliente(){

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
