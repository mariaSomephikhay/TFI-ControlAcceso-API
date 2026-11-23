package com.unla.accesscontrol.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.unla.accesscontrol.service.OperatorService;
import com.unla.accesscontrol.exceptions.UserException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/users")
public class OperatorController {
    private final OperatorService operators;

    public OperatorController(OperatorService operators) {
        this.operators = operators;
    }

    @PostMapping("/new")
    public ResponseEntity<String> newUser(@Valid @RequestBody CreateOperator request) {
        try {
            operators.create(request.username(), request.cardUid(), request.password());
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuario creado exitosamente");
        } catch (UserException | DataIntegrityViolationException duplicate) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al crear el usuario, El usuario ya existe.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody OperatorLogin request) {
        String uid = request.cardUid() == null ? request.id() : request.cardUid();
        boolean legacyRequest = request.cardUid() == null;
        if (uid == null || uid.isBlank() || uid.length() > (legacyRequest ? 255 : 64)
                || (request.id() != null && !request.id().equals(uid))
                || request.password() == null
                || !(request.password().isTextual() || request.password().isIntegralNumber())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error al iniciar sesion, Credenciales inválidas.");
        }

        try {
            // Retain the numeric PIN payload sent by deployed firmware.
            operators.login(uid, request.password().asText(), legacyRequest);
            return ResponseEntity.ok("Usuario logeado correctamente");
        } catch (UserException rejected) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error al iniciar sesion, " + rejected.getMessage());
        }
    }

    public record CreateOperator(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 64) String cardUid,
            @NotBlank @Size(max = 255) String password) {
    }

    public record OperatorLogin(String cardUid, String id, JsonNode password) {
    }
}
