package com.unla.gestionUsuario.service;

import org.springframework.web.multipart.MultipartFile;

import com.unla.gestionUsuario.entities.Patente;

public interface IPatenteService {
    Patente cargar(String numero);

    int importar(MultipartFile archivo);
}
