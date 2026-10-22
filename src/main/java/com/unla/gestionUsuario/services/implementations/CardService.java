package com.unla.gestionUsuario.services.implementations;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unla.gestionUsuario.entities.Card;
import com.unla.gestionUsuario.entities.User;
import com.unla.gestionUsuario.exceptions.CardException;
import com.unla.gestionUsuario.exceptions.CardException.Type;
import com.unla.gestionUsuario.repository.CardRepository;
import com.unla.gestionUsuario.repository.UserRepository;

@Service
public class CardService {
    private final CardRepository cards;
    private final UserRepository users;

    public CardService(CardRepository cards, UserRepository users) {
        this.cards = cards;
        this.users = users;
    }

    @Transactional
    public Card create(String userId, String code) {
        String normalized = normalize(code);
        User user = findUser(userId);
        if (cards.existsByCode(normalized)) {
            throw new CardException(Type.DUPLICATE, "La tarjeta ya está registrada.");
        }
        return cards.save(new Card(normalized, user));
    }

    @Transactional(readOnly = true)
    public List<Card> findByUser(String userId) {
        findUser(userId);
        return cards.findByUser_IdOrderByIdAsc(userId);
    }

    @Transactional
    public Card deactivate(String userId, Long cardId) {
        findUser(userId);
        Card card = cards.findById(cardId).orElseThrow(() -> cardNotFound(cardId));
        if (!card.getUser().getId().equals(userId)) {
            throw cardNotFound(cardId);
        }
        if (card.isActive()) {
            card.deactivate();
        }
        return card;
    }

    private User findUser(String userId) {
        return users.findById(userId).orElseThrow(() ->
                new CardException(Type.USER_NOT_FOUND, "No se encontró el usuario " + userId + "."));
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new CardException(Type.INVALID, "El código de la tarjeta es obligatorio.");
        }
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() > 64) {
            throw new CardException(Type.INVALID, "El código de la tarjeta no puede superar 64 caracteres.");
        }
        return normalized;
    }

    private CardException cardNotFound(Long cardId) {
        return new CardException(Type.CARD_NOT_FOUND, "No se encontró la tarjeta con ID " + cardId + ".");
    }
}
