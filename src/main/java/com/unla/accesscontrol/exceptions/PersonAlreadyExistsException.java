package com.unla.accesscontrol.exceptions;

public class PersonAlreadyExistsException extends RuntimeException {
    public PersonAlreadyExistsException(Throwable cause) {
        super("Ya existe un titular con ese documento.", cause);
    }
}
