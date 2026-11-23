package com.unla.accesscontrol;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.unla.accesscontrol.controller.OperatorController;
import com.unla.accesscontrol.service.OperatorService;
import com.unla.accesscontrol.exceptions.UserException;

class OperatorControllerTests {
    private final OperatorService service = mock(OperatorService.class);
    private final org.springframework.test.web.servlet.MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new OperatorController(service)).build();

    @Test
    void duplicateOperatorReturnsConflict() throws Exception {
        doThrow(UserException.of(UserException.Type.USER_ALREADY_EXISTS))
                .when(service).create("admin", "A01", "0123");

        mvc.perform(post("/users/new").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"cardUid\":\"A01\",\"password\":\"0123\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Error al crear el usuario, El usuario ya existe."));
    }

    @Test
    void loginPreservesLeadingZeroesAndAcceptsOldNumericPayload() throws Exception {
        mvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cardUid\":\"A01\",\"password\":\"0123\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"A02\",\"password\":1234}"))
                .andExpect(status().isOk());
        verify(service).login("A01", "0123", false);
        verify(service).login("A02", "1234", true);
    }
}
