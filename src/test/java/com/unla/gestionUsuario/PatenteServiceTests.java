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

import com.unla.gestionUsuario.entities.Patente;
import com.unla.gestionUsuario.exceptions.PatenteException;
import com.unla.gestionUsuario.exceptions.PatenteException.Type;
import com.unla.gestionUsuario.repository.PatenteRepository;
import com.unla.gestionUsuario.services.implementations.PatenteService;

class PatenteServiceTests {
    private PatenteRepository repository;
    private PatenteService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(PatenteRepository.class);
        service = new PatenteService(repository, 100_000, 1_000);
    }

    @Test
    void cargaIndividualNormalizaYGuarda() {
        when(repository.save(any(Patente.class))).thenAnswer(call -> call.getArgument(0));

        Patente creada = service.cargar(" ab123cd ");

        assertEquals("AB123CD", creada.getNumero());
        verify(repository).existsByNumero("AB123CD");
        verify(repository).save(any(Patente.class));
    }

    @Test
    void rechazaFormatoInvalidoYDuplicadoExistente() {
        PatenteException invalida = assertThrows(PatenteException.class, () -> service.cargar("AB-123"));
        assertEquals(Type.INVALIDA, invalida.getType());

        when(repository.existsByNumero("AB123CD")).thenReturn(true);
        PatenteException duplicada = assertThrows(PatenteException.class, () -> service.cargar("ab123cd"));
        assertEquals(Type.DUPLICADA, duplicada.getType());
        verify(repository, never()).save(any(Patente.class));
    }

    @Test
    void importaCsvConEncabezadoYNormalizaTodasLasFilas() {
        when(repository.findAllByNumeroIn(any())).thenReturn(List.of());

        int cantidad = service.importar(csv("\uFEFFpatente\n abc123 \n\"AB123CD\"\n"));

        assertEquals(2, cantidad);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Patente>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<String> numeros = new ArrayList<>();
        captor.getValue().forEach(patente -> numeros.add(patente.getNumero()));
        assertEquals(List.of("ABC123", "AB123CD"), numeros);
    }

    @Test
    void filaInvalidaImpideTodaLaImportacion() {
        PatenteException error = assertThrows(PatenteException.class,
                () -> service.importar(csv("patente\nABC123\nAB-123\n")));

        assertEquals(Type.INVALIDA, error.getType());
        assertTrue(error.getMessage().contains("Fila 3"));
        verify(repository, never()).saveAll(any());
    }

    @Test
    void duplicadosEnArchivoOBaseImpidenTodaLaImportacion() {
        PatenteException repetida = assertThrows(PatenteException.class,
                () -> service.importar(csv("ABC123\nabc123\n")));
        assertEquals(Type.DUPLICADA, repetida.getType());
        verify(repository, never()).saveAll(any());

        when(repository.findAllByNumeroIn(any())).thenReturn(List.of(new Patente("ABC123")));
        PatenteException existente = assertThrows(PatenteException.class,
                () -> service.importar(csv("ABC123\nAB123CD\n")));
        assertEquals(Type.DUPLICADA, existente.getType());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void respetaLosLimitesConfigurados() {
        PatenteService limiteArchivo = new PatenteService(repository, 5, 1_000);
        PatenteException archivoGrande = assertThrows(PatenteException.class,
                () -> limiteArchivo.importar(csv("ABC123\n")));
        assertEquals(Type.INVALIDA, archivoGrande.getType());
        assertTrue(archivoGrande.getMessage().contains("5 bytes"));

        PatenteService limiteFilas = new PatenteService(repository, 100, 1);
        PatenteException demasiadasFilas = assertThrows(PatenteException.class,
                () -> limiteFilas.importar(csv("ABC123\nAB123CD\n")));
        assertEquals(Type.INVALIDA, demasiadasFilas.getType());
        assertTrue(demasiadasFilas.getMessage().contains("(1)"));
        verify(repository, never()).saveAll(any());
    }

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "patentes.csv", "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }
}
