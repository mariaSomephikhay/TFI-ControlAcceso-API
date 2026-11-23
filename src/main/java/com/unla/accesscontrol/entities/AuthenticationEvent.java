package com.unla.accesscontrol.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "authentication_events")
public class AuthenticationEvent extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String event;

    @Column(nullable = false)
    private String description;

    protected AuthenticationEvent() {
    }

    public AuthenticationEvent(User user) {
        this.user = user;
        this.event = "0";
        this.description = "Usuario logeado";
    }
}
