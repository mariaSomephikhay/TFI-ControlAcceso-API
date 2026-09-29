package com.unla.gestionUsuario.repository;

import java.util.Collection;

import org.springframework.data.repository.CrudRepository;

import com.unla.gestionUsuario.entities.LicensePlate;

public interface LicensePlateRepository extends CrudRepository<LicensePlate, Long> {
    boolean existsByNumber(String number);

    Iterable<LicensePlate> findAllByNumberIn(Collection<String> numbers);
}
