package com.unla.gestionUsuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.unla.gestionUsuario.entities.Card;
import com.unla.gestionUsuario.entities.User;
import com.unla.gestionUsuario.exceptions.CardException;
import com.unla.gestionUsuario.exceptions.CardException.Type;
import com.unla.gestionUsuario.repository.CardRepository;
import com.unla.gestionUsuario.repository.UserRepository;
import com.unla.gestionUsuario.services.implementations.CardService;

class CardServiceTests {
    private CardRepository cards;
    private UserRepository users;
    private CardService service;
    private User owner;

    @BeforeEach
    void setUp() {
        cards = org.mockito.Mockito.mock(CardRepository.class);
        users = org.mockito.Mockito.mock(UserRepository.class);
        service = new CardService(cards, users);
        owner = new User();
        owner.setId("maria");
        when(users.findById("maria")).thenReturn(Optional.of(owner));
    }

    @Test
    void createsTwoCardsForOneUserAndListsThem() {
        when(cards.save(any(Card.class))).thenAnswer(call -> call.getArgument(0));

        Card first = service.create("maria", " ab12 ");
        Card second = service.create("maria", "cd34");
        when(cards.findByUser_IdOrderByIdAsc("maria")).thenReturn(List.of(first, second));

        assertSame(owner, first.getUser());
        assertSame(owner, second.getUser());
        assertEquals("AB12", first.getCode());
        assertEquals("CD34", second.getCode());
        assertTrue(first.isActive());
        assertEquals(List.of(first, second), service.findByUser("maria"));
    }

    @Test
    void rejectsInvalidDuplicateAndUnknownUser() {
        assertEquals(Type.INVALID, assertThrows(CardException.class,
                () -> service.create("maria", " ")).getType());
        assertEquals(Type.INVALID, assertThrows(CardException.class,
                () -> service.create("maria", "a".repeat(65))).getType());

        when(cards.existsByCode("AB12")).thenReturn(true);
        assertEquals(Type.DUPLICATE, assertThrows(CardException.class,
                () -> service.create("maria", "ab12")).getType());
        assertEquals(Type.USER_NOT_FOUND, assertThrows(CardException.class,
                () -> service.create("desconocido", "XY99")).getType());
        assertEquals(Type.USER_NOT_FOUND, assertThrows(CardException.class,
                () -> service.findByUser("desconocido")).getType());
        verify(cards, never()).save(any(Card.class));
    }

    @Test
    void deactivatesOwnedCardWithoutDeletingIt() {
        Card card = new Card("AB12", owner);
        when(cards.findById(7L)).thenReturn(Optional.of(card));

        assertSame(card, service.deactivate("maria", 7L));
        assertFalse(card.isActive());
        assertSame(card, service.deactivate("maria", 7L));
        verify(cards, never()).delete(any(Card.class));
    }

    @Test
    void refusesToDeactivateAnotherUsersCardOrMissingCard() {
        User other = new User();
        other.setId("otro");
        when(cards.findById(7L)).thenReturn(Optional.of(new Card("AB12", other)));

        assertEquals(Type.CARD_NOT_FOUND, assertThrows(CardException.class,
                () -> service.deactivate("maria", 7L)).getType());
        assertEquals(Type.CARD_NOT_FOUND, assertThrows(CardException.class,
                () -> service.deactivate("maria", 8L)).getType());
        assertEquals(Type.USER_NOT_FOUND, assertThrows(CardException.class,
                () -> service.deactivate("desconocido", 7L)).getType());
    }
}
