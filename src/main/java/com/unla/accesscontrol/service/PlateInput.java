package com.unla.accesscontrol.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import com.unla.gestionUsuario.config.LicensePlateProperties;
import com.unla.gestionUsuario.exceptions.LicensePlateException;
import com.unla.gestionUsuario.exceptions.LicensePlateException.Type;

@Component
public class PlateInput {
    private final List<Pattern> patterns;
    private final long maxFileBytes;
    private final int maxRows;

    public PlateInput(LicensePlateProperties properties,
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
        this.maxFileBytes = maxFileBytes;
        this.maxRows = maxRows;
    }

    public Set<String> readCsv(MultipartFile file) {
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

        return numbers;
    }

    public String normalize(String number) {
        if (number == null || number.isBlank()) {
            throw new LicensePlateException(Type.INVALID, "La patente es obligatoria.");
        }
        String normalized = number.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() > 16 || patterns.stream().noneMatch(pattern -> pattern.matcher(normalized).matches())) {
            throw new LicensePlateException(Type.INVALID,
                    "La patente no coincide con ningún formato permitido.");
        }
        return normalized;
    }
}
