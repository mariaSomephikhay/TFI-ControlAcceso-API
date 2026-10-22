package com.unla.gestionUsuario.services.implementations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.unla.gestionUsuario.config.LicensePlateProperties;
import com.unla.gestionUsuario.entities.LicensePlate;
import com.unla.gestionUsuario.exceptions.LicensePlateException;
import com.unla.gestionUsuario.exceptions.LicensePlateException.Type;
import com.unla.gestionUsuario.repository.LicensePlateRepository;
import com.unla.gestionUsuario.service.ILicensePlateService;

@Service
public class LicensePlateService implements ILicensePlateService {
    private final LicensePlateRepository repository;
    private final List<Pattern> patterns;
    private final long maxFileBytes;
    private final int maxRows;

    public LicensePlateService(LicensePlateRepository repository,
            LicensePlateProperties properties,
            @Value("${license-plates.import.max-file-bytes}") long maxFileBytes,
            @Value("${license-plates.import.max-rows}") int maxRows) {
        if (properties.patterns() == null || properties.patterns().isEmpty()) {
            throw new IllegalArgumentException("Debe configurarse al menos un patrón de patente.");
        }
        if (maxFileBytes <= 0 || maxRows <= 0) {
            throw new IllegalArgumentException("Los límites de importación deben ser mayores que cero.");
        }
        try {
            this.patterns = properties.patterns().stream().map(pattern -> {
                if (pattern == null || pattern.isBlank()) {
                    throw new IllegalArgumentException("Los patrones de patente no pueden estar vacíos.");
                }
                return Pattern.compile(pattern);
            }).toList();
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Patrón de patente inválido: " + e.getPattern(), e);
        }
        this.repository = repository;
        this.maxFileBytes = maxFileBytes;
        this.maxRows = maxRows;
    }

    @Override
    @Transactional
    public LicensePlate create(String number) {
        String normalized = normalize(number);
        if (repository.existsByNumber(normalized)) {
            throw new LicensePlateException(Type.DUPLICATE, "La patente ya está cargada: " + normalized);
        }
        return repository.save(new LicensePlate(normalized));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LicensePlate> findAll() {
        List<LicensePlate> plates = new ArrayList<>();
        repository.findAll().forEach(plates::add);
        return plates;
    }

    @Override
    @Transactional(readOnly = true)
    public LicensePlate findById(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Override
    @Transactional
    public LicensePlate update(Long id, String number) {
        LicensePlate plate = findById(id);
        String normalized = normalize(number);
        if (plate.getNumber().equals(normalized)) {
            return plate;
        }
        if (repository.existsByNumber(normalized)) {
            throw new LicensePlateException(Type.DUPLICATE, "La patente ya está cargada: " + normalized);
        }
        plate.changeNumber(normalized);
        return repository.save(plate);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    @Override
    @Transactional
    public int importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new LicensePlateException(Type.INVALID, "El archivo CSV está vacío.");
        }
        if (file.getSize() > maxFileBytes) {
            throw new LicensePlateException(Type.INVALID,
                    "El archivo CSV supera el límite de " + maxFileBytes + " bytes.");
        }

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new LicensePlateException(Type.INVALID, "No se pudo leer el archivo CSV.");
        }

        String[] lines = content.replaceFirst("^\\uFEFF", "").split("\\R", -1);
        Set<String> numbers = new LinkedHashSet<>();
        boolean firstRow = true;
        for (int i = 0; i < lines.length; i++) {
            String value = lines[i].strip();
            if (value.isEmpty()) {
                continue;
            }
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1).strip();
            }
            if (firstRow && value.equalsIgnoreCase("patente")) {
                firstRow = false;
                continue;
            }
            firstRow = false;
            if (numbers.size() >= maxRows) {
                throw new LicensePlateException(Type.INVALID,
                        "El archivo CSV supera el límite configurado de filas (" + maxRows + ").");
            }
            String number;
            try {
                number = normalize(value);
            } catch (LicensePlateException e) {
                throw new LicensePlateException(Type.INVALID, "Fila " + (i + 1) + ": " + e.getMessage());
            }
            if (!numbers.add(number)) {
                throw new LicensePlateException(Type.DUPLICATE,
                        "Fila " + (i + 1) + ": la patente aparece más de una vez: " + number);
            }
        }
        if (numbers.isEmpty()) {
            throw new LicensePlateException(Type.INVALID, "El archivo CSV no contiene patentes.");
        }

        for (LicensePlate existing : repository.findAllByNumberIn(numbers)) {
            throw new LicensePlateException(Type.DUPLICATE,
                    "La patente ya está cargada: " + existing.getNumber());
        }
        List<LicensePlate> newPlates = new ArrayList<>(numbers.size());
        for (String number : numbers) {
            newPlates.add(new LicensePlate(number));
        }
        repository.saveAll(newPlates);
        return newPlates.size();
    }

    private String normalize(String number) {
        if (number == null || number.isBlank()) {
            throw new LicensePlateException(Type.INVALID, "La patente es obligatoria.");
        }
        String normalized = number.strip().toUpperCase(Locale.ROOT);
        if (patterns.stream().noneMatch(pattern -> pattern.matcher(normalized).matches())) {
            throw new LicensePlateException(Type.INVALID,
                    "La patente no coincide con ningún formato permitido.");
        }
        return normalized;
    }

    private LicensePlateException notFound(Long id) {
        return new LicensePlateException(Type.NOT_FOUND, "No se encontró la patente con ID " + id + ".");
    }
}
