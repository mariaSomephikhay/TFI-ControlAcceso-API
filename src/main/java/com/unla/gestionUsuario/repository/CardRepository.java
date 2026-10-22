package com.unla.gestionUsuario.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.unla.gestionUsuario.entities.Card;

public interface CardRepository extends CrudRepository<Card, Long> {
    boolean existsByCode(String code);

    List<Card> findByUser_IdOrderByIdAsc(String userId);
}
