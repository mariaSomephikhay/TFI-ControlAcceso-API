package com.unla.gestionUsuario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.unla.accesscontrol.controller.CarPlateController;
import com.unla.accesscontrol.entities.Car;
import com.unla.gestionUsuario.exceptions.LicensePlateException;
import com.unla.gestionUsuario.exceptions.LicensePlateException.Type;
import com.unla.accesscontrol.service.CarService;

class CarPlateControllerTests {
    private CarService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(CarService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CarPlateController(service)).build();
    }

    @Test
    void createReturnsNormalizedNumber() throws Exception {
        when(service.create("ab123cd", 1L, null)).thenReturn(car("AB123CD"));

        mvc.perform(post("/patentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"ab123cd\",\"personId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("AB123CD"));
    }

    @Test
    void validationAndDuplicatesHaveDistinctStatuses() throws Exception {
        when(service.create("AB-123", 1L, null))
                .thenThrow(new LicensePlateException(Type.INVALID, "Formato inválido"));
        when(service.create("ABC123", 1L, null))
                .thenThrow(new LicensePlateException(Type.DUPLICATE, "Ya existe"));

        mvc.perform(post("/patentes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"AB-123\",\"personId\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/patentes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"ABC123\",\"personId\":1}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/patentes").contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void csvImportReturnsCount() throws Exception {
        when(service.importCsv(any(), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(2);
        MockMultipartFile file = new MockMultipartFile("archivo", "patentes.csv", "text/csv",
                "ABC123\nAB123CD\n".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/patentes/importar").file(file).param("personId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(2));
    }

    @Test
    void listsFindsUpdatesAndDeletesById() throws Exception {
        Car plate = car("AB123CD");
        ReflectionTestUtils.setField(plate, "id", 7L);
        when(service.findAll()).thenReturn(List.of(plate));
        when(service.findById(7L)).thenReturn(plate);
        Car updated = car("XY123");
        ReflectionTestUtils.setField(updated, "id", 7L);
        when(service.update(7L, "xy123")).thenReturn(updated);

        mvc.perform(get("/patentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].numero").value("AB123CD"));
        mvc.perform(get("/patentes/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
        mvc.perform(put("/patentes/7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"xy123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.numero").value("XY123"));
        mvc.perform(delete("/patentes/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void missingPlateReturnsNotFoundAndDuplicateUpdateReturnsConflict() throws Exception {
        when(service.findById(8L)).thenThrow(new LicensePlateException(Type.NOT_FOUND, "No existe"));
        when(service.update(7L, "AB123CD"))
                .thenThrow(new LicensePlateException(Type.DUPLICATE, "Ya existe"));

        mvc.perform(get("/patentes/8")).andExpect(status().isNotFound());
        mvc.perform(put("/patentes/7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"AB123CD\"}"))
                .andExpect(status().isConflict());
    }
    private Car car(String number) {
        Car car = new Car();
        car.setLicensePlate(number);
        return car;
    }
}
