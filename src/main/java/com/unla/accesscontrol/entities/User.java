package com.unla.accesscontrol.entities;

import org.hibernate.annotations.ColumnDefault;

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
@Table(name = "users")
public class User extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Setter
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Setter
    @Column(name = "card_uid", nullable = false, unique = true, length = 64)
    private String cardUid;

    @Setter
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Setter
    @ColumnDefault("0")
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Setter
    @ColumnDefault("true")
    @Column(name = "active", nullable = false, columnDefinition = "BOOLEAN")
    private boolean active = true;
}
