package com.unla.accesscontrol.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unla.accesscontrol.entities.Car;
import com.unla.accesscontrol.service.CarService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import com.unla.accesscontrol.dtos.LicensePlateDTO;
import com.unla.accesscontrol.dtos.LicensePlateImportDTO;
import com.unla.accesscontrol.exceptions.LicensePlateException;

@RestController
@RequestMapping("/patentes")
public class CarPlateController {
    private final CarService service;

    public CarPlateController(CarService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LicensePlateDTO> create(@Valid @RequestBody CreateCar request) {
        if (request == null) {
            throw new LicensePlateException(LicensePlateException.Type.INVALID, "La patente es obligatoria.");
        }
        Car plate = service.create(request.number(), request.personId(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(new LicensePlateDTO(plate.getLicensePlate()));
    }

    @GetMapping
    public List<PlateDetail> findAll() {
        return service.findAll().stream().map(PlateDetail::from).toList();
    }

    @GetMapping("/{id}")
    public PlateDetail findById(@PathVariable Long id) {
        return PlateDetail.from(service.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PlateDetail update(@PathVariable Long id, @RequestBody LicensePlateDTO request) {
        if (request == null) {
            throw new LicensePlateException(LicensePlateException.Type.INVALID, "La patente es obligatoria.");
        }
        return PlateDetail.from(service.update(id, request.number()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LicensePlateImportDTO> importCsv(@RequestPart("archivo") MultipartFile file,
            @RequestParam("personId") Long personId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new LicensePlateImportDTO(service.importCsv(file.getInputStream(), personId)));
        } catch (IOException error) {
            throw new LicensePlateException(LicensePlateException.Type.INVALID, "No se pudo leer el archivo CSV.", error);
        }
    }

    @ExceptionHandler(LicensePlateException.class)
    public ResponseEntity<String> handleLicensePlateError(LicensePlateException error) {
        HttpStatus status = switch (error.getType()) {
            case DUPLICATE -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(error.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleOversizedFile() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("El archivo CSV es demasiado grande.");
    }

    public record CreateCar(@JsonProperty("numero") String number, Long personId, @Size(max = 255) String description) {
    }

    public record PlateDetail(Long id, @JsonProperty("numero") String number,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        static PlateDetail from(Car car) {
            return new PlateDetail(car.getId(), car.getLicensePlate(), car.getCreatedAt(), car.getUpdatedAt());
        }
    }
}
