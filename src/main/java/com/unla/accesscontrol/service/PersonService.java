package com.unla.accesscontrol.service;

import java.util.List;
import java.util.Optional;

import com.unla.accesscontrol.entities.Person;

public interface PersonService {
    Person create(String firstName, String lastName, String nationalId, String email);

    List<Person> findAll();

    Optional<Person> findById(Long id);
}
