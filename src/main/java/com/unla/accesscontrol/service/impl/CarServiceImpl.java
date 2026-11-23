package com.unla.accesscontrol.service.impl;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unla.accesscontrol.entities.Car;
import com.unla.accesscontrol.entities.Person;
import com.unla.accesscontrol.repository.CarRepository;
import com.unla.accesscontrol.repository.PersonRepository;
import com.unla.accesscontrol.repository.TrackingRepository;
import com.unla.accesscontrol.exceptions.LicensePlateException;
import com.unla.accesscontrol.exceptions.LicensePlateException.Type;
import com.unla.accesscontrol.service.CarService;
import com.unla.accesscontrol.service.PlateInput;

@Service
public class CarServiceImpl implements CarService {
    private final CarRepository cars;
    private final PersonRepository people;
    private final TrackingRepository tracking;
    private final PlateInput input;

    public CarServiceImpl(CarRepository cars, PersonRepository people, TrackingRepository tracking, PlateInput input) {
        this.cars = cars;
        this.people = people;
        this.tracking = tracking;
        this.input = input;
    }

    @Override
    @Transactional
    public Car create(String number, Long personId, String description) {
        String normalized = input.normalize(number);
        Person person = findPerson(personId);
        rejectDuplicate(normalized);
        Car car = newCar(normalized, person);
        car.setDescription(description);
        try {
            return cars.saveAndFlush(car);
        } catch (DataIntegrityViolationException conflict) {
            throw plateConflict(conflict);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> findAll() {
        return cars.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Car findById(Long id) {
        return cars.findById(id).orElseThrow(() -> notFound(id));
    }

    @Override
    @Transactional
    public Car update(Long id, String number) {
        Car car = findById(id);
        String normalized = input.normalize(number);
        if (!car.getLicensePlate().equals(normalized)) {
            rejectDuplicate(normalized);
            car.setLicensePlate(normalized);
            try {
                cars.flush();
            } catch (DataIntegrityViolationException conflict) {
                throw plateConflict(conflict);
            }
        }
        return car;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Car car = cars.findForDeletion(id).orElseThrow(() -> notFound(id));
        try {
            tracking.detachCar(id);
            cars.delete(car);
            cars.flush();
        } catch (DataIntegrityViolationException conflict) {
            throw plateConflict(conflict);
        }
    }

    @Override
    @Transactional
    public int importCsv(InputStream file, Long personId) {
        Set<String> numbers = input.readCsv(file);
        Person person = findPerson(personId);
        cars.findFirstByLicensePlateIn(numbers).ifPresent(car -> {
            throw duplicate(car.getLicensePlate());
        });
        try {
            cars.saveAllAndFlush(numbers.stream().map(number -> newCar(number, person)).toList());
        } catch (DataIntegrityViolationException conflict) {
            throw plateConflict(conflict);
        }
        return numbers.size();
    }

    private Person findPerson(Long personId) {
        if (personId == null) {
            throw new LicensePlateException(Type.INVALID, "El titular (personId) es obligatorio.");
        }
        return people.findById(personId).orElseThrow(() ->
                new LicensePlateException(Type.NOT_FOUND, "No se encontró el titular con ID " + personId + "."));
    }

    private void rejectDuplicate(String number) {
        if (cars.existsByLicensePlate(number)) {
            throw duplicate(number);
        }
    }

    private Car newCar(String number, Person person) {
        Car car = new Car();
        car.setLicensePlate(number);
        car.setPerson(person);
        return car;
    }

    private LicensePlateException duplicate(String number) {
        return new LicensePlateException(Type.DUPLICATE, "La patente ya está cargada: " + number);
    }

    private LicensePlateException plateConflict(DataIntegrityViolationException cause) {
        return new LicensePlateException(Type.DUPLICATE, "Una patente ya está cargada.", cause);
    }

    private LicensePlateException notFound(Long id) {
        return new LicensePlateException(Type.NOT_FOUND, "No se encontró la patente con ID " + id + ".");
    }
}
