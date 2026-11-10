package com.gestioncomercial.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secuencia_cotizacion")
public class SecuenciaCotizacion {
    @Id
    private Long id;

    @Column(name = "ultimo_numero", nullable = false)
    private Long ultimoNumero;

    public SecuenciaCotizacion() {
    }

    public Long getId() {
        return id;
    }

    public Long getUltimoNumero() {
        return ultimoNumero;
    }

    public long siguienteNumero() {
        ultimoNumero = Math.incrementExact(ultimoNumero);
        return ultimoNumero;
    }
}
