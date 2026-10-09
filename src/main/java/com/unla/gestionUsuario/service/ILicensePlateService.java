package com.unla.gestionUsuario.service;

import org.springframework.web.multipart.MultipartFile;

import com.unla.gestionUsuario.entities.LicensePlate;

public interface ILicensePlateService {
    LicensePlate create(String number);

    int importCsv(MultipartFile file);
}
