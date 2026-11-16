package com.unla.accesscontrol.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "people")
public class Person extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Setter
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Setter
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Setter
    @Column(name = "email", length = 254)
    private String email;

    @Setter
    @Column(name = "national_id", nullable = false, unique = true, length = 20)
    private String nationalId;
}
