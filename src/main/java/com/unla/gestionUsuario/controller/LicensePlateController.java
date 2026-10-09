package com.unla.gestionUsuario.controller;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.unla.gestionUsuario.dtos.LicensePlateDetailDTO;
import com.unla.gestionUsuario.dtos.LicensePlateDTO;
import com.unla.gestionUsuario.dtos.LicensePlateImportDTO;
import com.unla.gestionUsuario.entities.LicensePlate;
import com.unla.gestionUsuario.exceptions.LicensePlateException;
import com.unla.gestionUsuario.service.ILicensePlateService;

@RestController
@RequestMapping("/patentes")
public class LicensePlateController {
    private final ILicensePlateService service;

    public LicensePlateController(ILicensePlateService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LicensePlateDTO> create(@RequestBody LicensePlateDTO request) {
        if (request == null) {
            throw new LicensePlateException(LicensePlateException.Type.INVALID, "La patente es obligatoria.");
        }
        LicensePlate plate = service.create(request.number());
        return ResponseEntity.status(HttpStatus.CREATED).body(new LicensePlateDTO(plate.getNumber()));
    }

    @GetMapping
    public List<LicensePlateDetailDTO> findAll() {
        return service.findAll().stream().map(LicensePlateDetailDTO::from).toList();
    }

    @GetMapping("/{id}")
    public LicensePlateDetailDTO findById(@PathVariable Long id) {
        return LicensePlateDetailDTO.from(service.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LicensePlateDetailDTO update(@PathVariable Long id, @RequestBody LicensePlateDTO request) {
        if (request == null) {
            throw new LicensePlateException(LicensePlateException.Type.INVALID, "La patente es obligatoria.");
        }
        return LicensePlateDetailDTO.from(service.update(id, request.number()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LicensePlateImportDTO> importCsv(@RequestPart("archivo") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new LicensePlateImportDTO(service.importCsv(file)));
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleConcurrentDuplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Una patente ya está cargada.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleOversizedFile() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("El archivo CSV es demasiado grande.");
    }
}
