package com.unla.gestionUsuario.repository;

import java.util.Collection;

import org.springframework.data.repository.CrudRepository;

import com.unla.gestionUsuario.entities.Patente;

public interface PatenteRepository extends CrudRepository<Patente, Long> {
    boolean existsByNumero(String numero);

    Iterable<Patente> findAllByNumeroIn(Collection<String> numeros);
}
