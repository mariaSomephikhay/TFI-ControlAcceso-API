package com.unla.accesscontrol;

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

import com.unla.accesscontrol.config.LicensePlateProperties;
import com.unla.accesscontrol.entities.Car;
import com.unla.accesscontrol.entities.Person;
import com.unla.accesscontrol.exceptions.LicensePlateException;
import com.unla.accesscontrol.exceptions.LicensePlateException.Type;
import com.unla.accesscontrol.repository.CarRepository;
import com.unla.accesscontrol.repository.PersonRepository;
import com.unla.accesscontrol.repository.TrackingRepository;
import com.unla.accesscontrol.service.CarService;
import com.unla.accesscontrol.service.PlateInput;

class CarServiceTests {
    private static final LicensePlateProperties DEFAULT_PROPERTIES =
            new LicensePlateProperties(List.of("[A-Z0-9]{1,16}"));

    private CarRepository repository;
    private CarService service;
    private PersonRepository people;
    private TrackingRepository tracking;
    private Person owner;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(CarRepository.class);
        people = org.mockito.Mockito.mock(PersonRepository.class);
        tracking = org.mockito.Mockito.mock(TrackingRepository.class);
        owner = new Person();
        when(people.findById(1L)).thenReturn(java.util.Optional.of(owner));
        service = withInput(DEFAULT_PROPERTIES, 100_000, 1_000);
    }

    private CarService withInput(LicensePlateProperties properties, long bytes, int rows) {
        return new CarService(repository, people, tracking, new PlateInput(properties, bytes, rows));
    }

    private Car car(String number) {
        Car car = new Car();
        car.setPerson(owner);
        car.setLicensePlate(number);
        return car;
    }

    @Test
    void createNormalizesAndSaves() {
        when(repository.saveAndFlush(any(Car.class))).thenAnswer(call -> call.getArgument(0));

        Car created = service.create(" ab123cd ", 1L, null);

        assertEquals("AB123CD", created.getLicensePlate());
        assertEquals(owner, created.getPerson());
        verify(repository).existsByLicensePlate("AB123CD");
        verify(repository).saveAndFlush(any(Car.class));
    }

    @Test
    void rejectsInvalidFormatAndExistingNumber() {
        LicensePlateException invalid = assertThrows(LicensePlateException.class, () -> service.create("AB-123", 1L, null));
        assertEquals(Type.INVALID, invalid.getType());

        when(repository.existsByLicensePlate("AB123CD")).thenReturn(true);
        LicensePlateException duplicate = assertThrows(LicensePlateException.class, () -> service.create("ab123cd", 1L, null));
        assertEquals(Type.DUPLICATE, duplicate.getType());
        verify(repository, never()).saveAndFlush(any(Car.class));
    }

    @Test
    void acceptsAnyConfiguredPatternForIndividualAndCsvImport() {
        LicensePlateProperties properties = new LicensePlateProperties(
                List.of("[A-Z]{2}[0-9]{3}", "[0-9]{3}[A-Z]{2}"));
        service = withInput(properties, 100_000, 1_000);
        when(repository.saveAndFlush(any(Car.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.findFirstByLicensePlateIn(any())).thenReturn(java.util.Optional.empty());

        assertEquals("AB123", service.create("ab123", 1L, null).getLicensePlate());
        assertEquals("123AB", service.create("123ab", 1L, null).getLicensePlate());
        assertEquals(2, service.importCsv(csv("AB123\n123AB\n"), 1L));
        assertEquals(Type.INVALID, assertThrows(LicensePlateException.class,
                () -> service.create("AB123CD", 1L, null)).getType());
        assertEquals(Type.INVALID, assertThrows(LicensePlateException.class,
                () -> service.importCsv(csv("AB123CD\n"), 1L)).getType());
    }

    @Test
    void rejectsMissingOrInvalidConfiguredPatterns() {
        assertThrows(IllegalArgumentException.class,
                () -> withInput(new LicensePlateProperties(List.of()), 100, 10));
        assertThrows(IllegalArgumentException.class,
                () -> withInput(new LicensePlateProperties(List.of("[A-Z")), 100, 10));
    }

    @Test
    void findsPlatesAndReportsMissingId() {
        Car plate = car("AB123CD");
        when(repository.findAll()).thenReturn(List.of(plate));
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(plate));

        assertEquals(List.of(plate), service.findAll());
        assertEquals(plate, service.findById(7L));
        assertEquals(Type.NOT_FOUND, assertThrows(LicensePlateException.class,
                () -> service.findById(8L)).getType());
    }

    @Test
    void updateNormalizesAndRejectsDuplicatesWithoutChangingStoredNumber() {
        Car plate = car("AB123CD");
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(plate));

        assertEquals("XY123", service.update(7L, " xy123 ").getLicensePlate());
        verify(repository).flush();

        assertEquals("XY123", service.update(7L, "xy123").getLicensePlate());
        verify(repository).flush();

        when(repository.existsByLicensePlate("ZZ999")).thenReturn(true);
        assertEquals(Type.DUPLICATE, assertThrows(LicensePlateException.class,
                () -> service.update(7L, "zz999")).getType());
        assertEquals("XY123", plate.getLicensePlate());
        verify(repository).flush();
        assertEquals(Type.NOT_FOUND, assertThrows(LicensePlateException.class,
                () -> service.update(8L, "AA123")).getType());
    }

    @Test
    void deleteRequiresAnExistingPlate() {
        Car plate = car("AB123CD");
        when(repository.findForDeletion(7L)).thenReturn(java.util.Optional.of(plate));

        service.delete(7L);

        verify(tracking).detachCar(7L);
        verify(repository).delete(plate);
        assertEquals(Type.NOT_FOUND, assertThrows(LicensePlateException.class,
                () -> service.delete(8L)).getType());
    }

    @Test
    void csvImportNormalizesRows() {
        when(repository.findFirstByLicensePlateIn(any())).thenReturn(java.util.Optional.empty());

        int count = service.importCsv(csv("\uFEFFpatente\n abc123 \n\"AB123CD\"\n"), 1L);

        assertEquals(2, count);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Car>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAllAndFlush(captor.capture());
        List<String> numbers = new ArrayList<>();
        captor.getValue().forEach(plate -> numbers.add(plate.getLicensePlate()));
        assertEquals(List.of("ABC123", "AB123CD"), numbers);
    }

    @Test
    void invalidRowPreventsImport() {
        LicensePlateException error = assertThrows(LicensePlateException.class,
                () -> service.importCsv(csv("patente\nABC123\nAB-123\n"), 1L));

        assertEquals(Type.INVALID, error.getType());
        assertTrue(error.getMessage().contains("Fila 3"));
        verify(repository, never()).saveAllAndFlush(any());
    }

    @Test
    void duplicatesInFileOrDatabasePreventImport() {
        LicensePlateException repeated = assertThrows(LicensePlateException.class,
                () -> service.importCsv(csv("ABC123\nabc123\n"), 1L));
        assertEquals(Type.DUPLICATE, repeated.getType());
        verify(repository, never()).saveAllAndFlush(any());

        when(repository.findFirstByLicensePlateIn(any())).thenReturn(java.util.Optional.of(car("ABC123")));
        LicensePlateException existing = assertThrows(LicensePlateException.class,
                () -> service.importCsv(csv("ABC123\nAB123CD\n"), 1L));
        assertEquals(Type.DUPLICATE, existing.getType());
        verify(repository, never()).saveAllAndFlush(any());
    }

    @Test
    void respectsConfiguredLimits() {
        CarService fileLimit = withInput(DEFAULT_PROPERTIES, 5, 1_000);
        LicensePlateException oversizedFile = assertThrows(LicensePlateException.class,
                () -> fileLimit.importCsv(csv("ABC123\n"), 1L));
        assertEquals(Type.INVALID, oversizedFile.getType());
        assertTrue(oversizedFile.getMessage().contains("5 bytes"));

        CarService rowLimit = withInput(DEFAULT_PROPERTIES, 100, 1);
        LicensePlateException tooManyRows = assertThrows(LicensePlateException.class,
                () -> rowLimit.importCsv(csv("ABC123\nAB123CD\n"), 1L));
        assertEquals(Type.INVALID, tooManyRows.getType());
        assertTrue(tooManyRows.getMessage().contains("(1)"));
        verify(repository, never()).saveAllAndFlush(any());
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("archivo", "patentes.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
