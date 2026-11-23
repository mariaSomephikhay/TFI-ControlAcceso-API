package com.unla.accesscontrol.exceptions;

public class LicensePlateException extends RuntimeException {
    public enum Type {
        INVALID,
        DUPLICATE,
        NOT_FOUND
    }

    private final Type type;

    public LicensePlateException(Type type, String message) {
        this(type, message, null);
    }

    public LicensePlateException(Type type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
