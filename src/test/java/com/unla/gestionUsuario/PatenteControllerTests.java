package com.unla.gestionUsuario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.unla.gestionUsuario.controller.PatenteController;
import com.unla.gestionUsuario.entities.Patente;
import com.unla.gestionUsuario.exceptions.PatenteException;
import com.unla.gestionUsuario.exceptions.PatenteException.Type;
import com.unla.gestionUsuario.service.IPatenteService;

class PatenteControllerTests {
    private IPatenteService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(IPatenteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PatenteController(service)).build();
    }

    @Test
    void altaIndividualDevuelveCreadoYNumeroNormalizado() throws Exception {
        when(service.cargar("ab123cd")).thenReturn(new Patente("AB123CD"));

        mvc.perform(post("/patentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"ab123cd\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("AB123CD"));
    }

    @Test
    void validacionYDuplicadoTienenEstadosDistintos() throws Exception {
        when(service.cargar("AB-123"))
                .thenThrow(new PatenteException(Type.INVALIDA, "Formato inválido"));
        when(service.cargar("ABC123"))
                .thenThrow(new PatenteException(Type.DUPLICADA, "Ya existe"));

        mvc.perform(post("/patentes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"AB-123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/patentes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"ABC123\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/patentes").contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importacionCsvDevuelveCantidad() throws Exception {
        when(service.importar(any())).thenReturn(2);
        MockMultipartFile archivo = new MockMultipartFile("archivo", "patentes.csv", "text/csv",
                "ABC123\nAB123CD\n".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/patentes/importar").file(archivo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(2));
    }
}
