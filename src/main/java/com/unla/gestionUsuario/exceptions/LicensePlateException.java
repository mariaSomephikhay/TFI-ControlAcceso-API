package com.unla.gestionUsuario.exceptions;

public class LicensePlateException extends RuntimeException {
    public enum Type {
        INVALID,
        DUPLICATE
    }

    private final Type type;

    public LicensePlateException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
