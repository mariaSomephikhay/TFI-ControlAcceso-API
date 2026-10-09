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

import com.unla.gestionUsuario.controller.LicensePlateController;
import com.unla.gestionUsuario.entities.LicensePlate;
import com.unla.gestionUsuario.exceptions.LicensePlateException;
import com.unla.gestionUsuario.exceptions.LicensePlateException.Type;
import com.unla.gestionUsuario.service.ILicensePlateService;

class LicensePlateControllerTests {
    private ILicensePlateService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(ILicensePlateService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LicensePlateController(service)).build();
    }

    @Test
    void createReturnsNormalizedNumber() throws Exception {
        when(service.create("ab123cd")).thenReturn(new LicensePlate("AB123CD"));

        mvc.perform(post("/patentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"ab123cd\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("AB123CD"));
    }

    @Test
    void validationAndDuplicatesHaveDistinctStatuses() throws Exception {
        when(service.create("AB-123"))
                .thenThrow(new LicensePlateException(Type.INVALID, "Formato inválido"));
        when(service.create("ABC123"))
                .thenThrow(new LicensePlateException(Type.DUPLICATE, "Ya existe"));

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
    void csvImportReturnsCount() throws Exception {
        when(service.importCsv(any())).thenReturn(2);
        MockMultipartFile file = new MockMultipartFile("archivo", "patentes.csv", "text/csv",
                "ABC123\nAB123CD\n".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/patentes/importar").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(2));
    }
}
