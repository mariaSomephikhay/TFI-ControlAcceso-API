package com.unla.gestionUsuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import com.unla.gestionUsuario.entities.LicensePlate;
import com.unla.gestionUsuario.exceptions.LicensePlateException;
import com.unla.gestionUsuario.exceptions.LicensePlateException.Type;
import com.unla.gestionUsuario.repository.LicensePlateRepository;
import com.unla.gestionUsuario.services.implementations.LicensePlateService;

class LicensePlateServiceTests {
    private LicensePlateRepository repository;
    private LicensePlateService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(LicensePlateRepository.class);
        service = new LicensePlateService(repository, 100_000, 1_000);
    }

    @Test
    void createNormalizesAndSaves() {
        when(repository.save(any(LicensePlate.class))).thenAnswer(call -> call.getArgument(0));

        LicensePlate created = service.create(" ab123cd ");

        assertEquals("AB123CD", created.getNumber());
        verify(repository).existsByNumber("AB123CD");
        verify(repository).save(any(LicensePlate.class));
    }

    @Test
    void rejectsInvalidFormatAndExistingNumber() {
        LicensePlateException invalid = assertThrows(LicensePlateException.class, () -> service.create("AB-123"));
        assertEquals(Type.INVALID, invalid.getType());

        when(repository.existsByNumber("AB123CD")).thenReturn(true);
        LicensePlateException duplicate = assertThrows(LicensePlateException.class, () -> service.create("ab123cd"));
        assertEquals(Type.DUPLICATE, duplicate.getType());
        verify(repository, never()).save(any(LicensePlate.class));
    }

    @Test
    void csvImportNormalizesRows() {
        when(repository.findAllByNumberIn(any())).thenReturn(List.of());

        int count = service.importCsv(csv("\uFEFFpatente\n abc123 \n\"AB123CD\"\n"));

        assertEquals(2, count);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<LicensePlate>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<String> numbers = new ArrayList<>();
        captor.getValue().forEach(plate -> numbers.add(plate.getNumber()));
        assertEquals(List.of("ABC123", "AB123CD"), numbers);
    }

    @Test
    void invalidRowPreventsImport() {
        LicensePlateException error = assertThrows(LicensePlateException.class,
                () -> service.importCsv(csv("patente\nABC123\nAB-123\n")));

        assertEquals(Type.INVALID, error.getType());
        assertTrue(error.getMessage().contains("Fila 3"));
        verify(repository, never()).saveAll(any());
    }

    @Test
    void duplicatesInFileOrDatabasePreventImport() {
        LicensePlateException repeated = assertThrows(LicensePlateException.class,
                () -> service.importCsv(csv("ABC123\nabc123\n")));
        assertEquals(Type.DUPLICATE, repeated.getType());
        verify(repository, never()).saveAll(any());

        when(repository.findAllByNumberIn(any())).thenReturn(List.of(new LicensePlate("ABC123")));
        LicensePlateException existing = assertThrows(LicensePlateException.class,
                () -> service.importCsv(csv("ABC123\nAB123CD\n")));
        assertEquals(Type.DUPLICATE, existing.getType());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void respectsConfiguredLimits() {
        LicensePlateService fileLimit = new LicensePlateService(repository, 5, 1_000);
        LicensePlateException oversizedFile = assertThrows(LicensePlateException.class,
                () -> fileLimit.importCsv(csv("ABC123\n")));
        assertEquals(Type.INVALID, oversizedFile.getType());
        assertTrue(oversizedFile.getMessage().contains("5 bytes"));

        LicensePlateService rowLimit = new LicensePlateService(repository, 100, 1);
        LicensePlateException tooManyRows = assertThrows(LicensePlateException.class,
                () -> rowLimit.importCsv(csv("ABC123\nAB123CD\n")));
        assertEquals(Type.INVALID, tooManyRows.getType());
        assertTrue(tooManyRows.getMessage().contains("(1)"));
        verify(repository, never()).saveAll(any());
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("archivo", "patentes.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
