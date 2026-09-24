package com.unla.gestionUsuario.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.unla.gestionUsuario.dtos.ImportacionPatentesDTO;
import com.unla.gestionUsuario.dtos.PatenteDTO;
import com.unla.gestionUsuario.entities.Patente;
import com.unla.gestionUsuario.exceptions.PatenteException;
import com.unla.gestionUsuario.service.IPatenteService;

@RestController
@RequestMapping("/patentes")
public class PatenteController {
    private final IPatenteService service;

    public PatenteController(IPatenteService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatenteDTO> cargar(@RequestBody PatenteDTO request) {
        if (request == null) {
            throw new PatenteException(PatenteException.Type.INVALIDA, "La patente es obligatoria.");
        }
        Patente patente = service.cargar(request.numero());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PatenteDTO(patente.getNumero()));
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacionPatentesDTO> importar(@RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImportacionPatentesDTO(service.importar(archivo)));
    }

    @ExceptionHandler(PatenteException.class)
    public ResponseEntity<String> errorDePatente(PatenteException error) {
        HttpStatus status = error.getType() == PatenteException.Type.DUPLICADA
                ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(error.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> patenteConcurrente() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Una patente ya está cargada.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> archivoDemasiadoGrande() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("El archivo CSV es demasiado grande.");
    }
}
