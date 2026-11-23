package com.unla.accesscontrol.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LicensePlateImportDTO(@JsonProperty("cantidad") int count) {
}
