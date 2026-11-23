package com.unla.accesscontrol.service;

import java.io.InputStream;
import java.util.List;

import com.unla.accesscontrol.entities.Car;

public interface CarService {
    Car create(String number, Long personId, String description);

    List<Car> findAll();

    Car findById(Long id);

    Car update(Long id, String number);

    void delete(Long id);

    /** Reads and closes the CSV stream before persisting the imported plates. */
    int importCsv(InputStream file, Long personId);
}
