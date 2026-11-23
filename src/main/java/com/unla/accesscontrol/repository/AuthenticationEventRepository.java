package com.unla.accesscontrol.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unla.accesscontrol.entities.AuthenticationEvent;

public interface AuthenticationEventRepository extends JpaRepository<AuthenticationEvent, Long> {
}
