package com.unla.accesscontrol.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unla.accesscontrol.entities.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
