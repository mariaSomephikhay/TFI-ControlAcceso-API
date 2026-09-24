package com.unla.gestionUsuario.services.implementations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.unla.gestionUsuario.entities.Patente;
import com.unla.gestionUsuario.exceptions.PatenteException;
import com.unla.gestionUsuario.exceptions.PatenteException.Type;
import com.unla.gestionUsuario.repository.PatenteRepository;
import com.unla.gestionUsuario.service.IPatenteService;

@Service
public class PatenteService implements IPatenteService {
    private static final Pattern FORMATO = Pattern.compile("[A-Z0-9]{1,16}");
    private static final long MAX_ARCHIVO_BYTES = 100_000;
    private static final int MAX_FILAS = 1_000;

    private final PatenteRepository repository;

    public PatenteService(PatenteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Patente cargar(String numero) {
        String normalizado = normalizar(numero);
        if (repository.existsById(normalizado)) {
            throw new PatenteException(Type.DUPLICADA, "La patente ya está cargada: " + normalizado);
        }
        return repository.save(new Patente(normalizado));
    }

    @Override
    @Transactional
    public int importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new PatenteException(Type.INVALIDA, "El archivo CSV está vacío.");
        }
        if (archivo.getSize() > MAX_ARCHIVO_BYTES) {
            throw new PatenteException(Type.INVALIDA, "El archivo CSV supera los 100 KB.");
        }

        String contenido;
        try {
            contenido = new String(archivo.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PatenteException(Type.INVALIDA, "No se pudo leer el archivo CSV.");
        }

        String[] lineas = contenido.replaceFirst("^\\uFEFF", "").split("\\R", -1);
        Set<String> numeros = new LinkedHashSet<>();
        boolean primeraFila = true;
        for (int i = 0; i < lineas.length; i++) {
            String valor = lineas[i].strip();
            if (valor.isEmpty()) {
                continue;
            }
            if (valor.startsWith("\"") && valor.endsWith("\"") && valor.length() >= 2) {
                valor = valor.substring(1, valor.length() - 1).strip();
            }
            if (primeraFila && valor.equalsIgnoreCase("patente")) {
                primeraFila = false;
                continue;
            }
            primeraFila = false;
            if (numeros.size() >= MAX_FILAS) {
                throw new PatenteException(Type.INVALIDA, "El archivo CSV supera las 1000 patentes.");
            }
            String numero;
            try {
                numero = normalizar(valor);
            } catch (PatenteException e) {
                throw new PatenteException(Type.INVALIDA, "Fila " + (i + 1) + ": " + e.getMessage());
            }
            if (!numeros.add(numero)) {
                throw new PatenteException(Type.DUPLICADA,
                        "Fila " + (i + 1) + ": la patente aparece más de una vez: " + numero);
            }
        }
        if (numeros.isEmpty()) {
            throw new PatenteException(Type.INVALIDA, "El archivo CSV no contiene patentes.");
        }

        for (Patente existente : repository.findAllById(numeros)) {
            throw new PatenteException(Type.DUPLICADA,
                    "La patente ya está cargada: " + existente.getNumero());
        }
        List<Patente> nuevas = new ArrayList<>(numeros.size());
        for (String numero : numeros) {
            nuevas.add(new Patente(numero));
        }
        repository.saveAll(nuevas);
        return nuevas.size();
    }

    private String normalizar(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new PatenteException(Type.INVALIDA, "La patente es obligatoria.");
        }
        String normalizado = numero.strip().toUpperCase(Locale.ROOT);
        if (!FORMATO.matcher(normalizado).matches()) {
            throw new PatenteException(Type.INVALIDA,
                    "La patente debe tener de 1 a 16 letras o números, sin espacios ni símbolos.");
        }
        return normalizado;
    }
}
