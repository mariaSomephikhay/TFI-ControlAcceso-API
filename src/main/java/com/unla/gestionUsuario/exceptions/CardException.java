package com.unla.gestionUsuario.exceptions;

public class CardException extends RuntimeException {
    public enum Type { INVALID, DUPLICATE, USER_NOT_FOUND, CARD_NOT_FOUND }

    private final Type type;

    public CardException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
