package com.unla.gestionUsuario;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.unla.gestionUsuario.controller.CardController;
import com.unla.gestionUsuario.entities.Card;
import com.unla.gestionUsuario.entities.User;
import com.unla.gestionUsuario.exceptions.CardException;
import com.unla.gestionUsuario.exceptions.CardException.Type;
import com.unla.gestionUsuario.services.implementations.CardService;

class CardControllerTests {
    private CardService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(CardService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CardController(service)).build();
    }

    @Test
    void createsListsAndDeactivatesUsersCard() throws Exception {
        User user = new User();
        user.setId("maria");
        Card card = new Card("AB12", user);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(service.create("maria", "ab12")).thenReturn(card);
        when(service.findByUser("maria")).thenReturn(List.of(card));

        mvc.perform(post("/users/maria/cards").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ab12\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.code").value("AB12"))
                .andExpect(jsonPath("$.userId").value("maria"))
                .andExpect(jsonPath("$.active").value(true));
        mvc.perform(get("/users/maria/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));

        card.deactivate();
        when(service.deactivate("maria", 7L)).thenReturn(card);
        mvc.perform(patch("/users/maria/cards/7/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void mapsValidationDuplicateAndMissingRecordsToHttpStatuses() throws Exception {
        when(service.create("maria", "AB12"))
                .thenThrow(new CardException(Type.DUPLICATE, "Duplicada"));
        when(service.findByUser("desconocido"))
                .thenThrow(new CardException(Type.USER_NOT_FOUND, "Usuario inexistente"));
        when(service.deactivate("maria", 8L))
                .thenThrow(new CardException(Type.CARD_NOT_FOUND, "Tarjeta inexistente"));

        mvc.perform(post("/users/maria/cards").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/users/maria/cards").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"AB12\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/users/desconocido/cards")).andExpect(status().isNotFound());
        mvc.perform(patch("/users/maria/cards/8/deactivate")).andExpect(status().isNotFound());
    }
}
