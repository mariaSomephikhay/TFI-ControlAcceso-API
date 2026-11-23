package com.unla.accesscontrol.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unla.accesscontrol.entities.Person;
import com.unla.accesscontrol.exceptions.PersonAlreadyExistsException;
import com.unla.accesscontrol.repository.PersonRepository;
import com.unla.accesscontrol.service.PersonService;

@Service
public class PersonServiceImpl implements PersonService {
    private final PersonRepository people;

    public PersonServiceImpl(PersonRepository people) {
        this.people = people;
    }

    @Override
    @Transactional
    public Person create(String firstName, String lastName, String nationalId, String email) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setNationalId(nationalId);
        person.setEmail(email);
        try {
            return people.saveAndFlush(person);
        } catch (DataIntegrityViolationException conflict) {
            throw new PersonAlreadyExistsException(conflict);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> findAll() {
        return people.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Person> findById(Long id) {
        return people.findById(id);
    }
}
