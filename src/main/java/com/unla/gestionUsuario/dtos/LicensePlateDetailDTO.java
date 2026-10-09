package com.unla.gestionUsuario.dtos;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unla.gestionUsuario.entities.LicensePlate;

public record LicensePlateDetailDTO(Long id, @JsonProperty("numero") String number,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static LicensePlateDetailDTO from(LicensePlate plate) {
        return new LicensePlateDetailDTO(plate.getId(), plate.getNumber(),
                plate.getCreatedAt(), plate.getUpdatedAt());
    }
}
