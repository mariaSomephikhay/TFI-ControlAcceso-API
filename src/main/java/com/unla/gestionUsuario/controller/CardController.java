package com.unla.gestionUsuario.controller;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unla.gestionUsuario.dtos.CardDetailDTO;
import com.unla.gestionUsuario.dtos.CardRequestDTO;
import com.unla.gestionUsuario.exceptions.CardException;
import com.unla.gestionUsuario.services.implementations.CardService;

@RestController
@RequestMapping("/users/{userId}/cards")
public class CardController {
    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CardDetailDTO> create(@PathVariable String userId, @RequestBody CardRequestDTO request) {
        if (request == null) {
            throw new CardException(CardException.Type.INVALID, "El código de la tarjeta es obligatorio.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(CardDetailDTO.from(service.create(userId, request.code()), userId));
    }

    @GetMapping
    public List<CardDetailDTO> findByUser(@PathVariable String userId) {
        return service.findByUser(userId).stream().map(card -> CardDetailDTO.from(card, userId)).toList();
    }

    @PatchMapping("/{cardId}/deactivate")
    public CardDetailDTO deactivate(@PathVariable String userId, @PathVariable Long cardId) {
        return CardDetailDTO.from(service.deactivate(userId, cardId), userId);
    }

    @ExceptionHandler(CardException.class)
    public ResponseEntity<String> handleCardError(CardException error) {
        HttpStatus status = switch (error.getType()) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case DUPLICATE -> HttpStatus.CONFLICT;
            case USER_NOT_FOUND, CARD_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(status).body(error.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleConcurrentDuplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("La tarjeta ya está registrada.");
    }
}
