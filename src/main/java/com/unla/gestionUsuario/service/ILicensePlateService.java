package com.unla.gestionUsuario.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.unla.gestionUsuario.entities.LicensePlate;

public interface ILicensePlateService {
    LicensePlate create(String number);

    List<LicensePlate> findAll();

    LicensePlate findById(Long id);

    LicensePlate update(Long id, String number);

    void delete(Long id);

    int importCsv(MultipartFile file);
}
