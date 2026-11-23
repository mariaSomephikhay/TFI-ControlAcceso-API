package com.unla.accesscontrol.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unla.accesscontrol.entities.Person;
import com.unla.accesscontrol.exceptions.PersonAlreadyExistsException;
import com.unla.accesscontrol.service.PersonService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/people")
public class PersonController {
    private final PersonService people;

    public PersonController(PersonService people) {
        this.people = people;
    }

    @PostMapping
    public ResponseEntity<PersonDetails> create(@Valid @RequestBody CreatePerson request) {
        try {
            Person person = people.create(request.firstName(), request.lastName(), request.nationalId(), request.email());
            return ResponseEntity.status(HttpStatus.CREATED).body(PersonDetails.from(person));
        } catch (PersonAlreadyExistsException duplicate) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping
    public List<PersonDetails> list() {
        return people.findAll().stream().map(PersonDetails::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDetails> find(@PathVariable Long id) {
        return ResponseEntity.of(people.findById(id).map(PersonDetails::from));
    }

    public record CreatePerson(@NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName, @NotBlank @Size(max = 20) String nationalId,
            @Email @Size(max = 254) String email) {
    }

    public record PersonDetails(Long id, String firstName, String lastName, String nationalId, String email) {
        static PersonDetails from(Person person) {
            return new PersonDetails(person.getId(), person.getFirstName(), person.getLastName(),
                    person.getNationalId(), person.getEmail());
        }
    }
}
