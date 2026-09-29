package com.unla.gestionUsuario.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patentes")
public class Patente {
    @Id
    @Column(name = "numero", nullable = false, length = 16)
    private String numero;

    protected Patente() {
    }

    public Patente(String numero) {
        this.numero = numero;
    }

    public String getNumero() {
        return numero;
    }
}
