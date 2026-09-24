package com.unla.gestionUsuario.exceptions;

public class PatenteException extends RuntimeException {
    public enum Type {
        INVALIDA,
        DUPLICADA
    }

    private final Type type;

    public PatenteException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
