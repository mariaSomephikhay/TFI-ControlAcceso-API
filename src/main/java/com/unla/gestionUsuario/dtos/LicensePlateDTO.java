package com.unla.gestionUsuario.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LicensePlateDTO(@JsonProperty("numero") String number) {
}
