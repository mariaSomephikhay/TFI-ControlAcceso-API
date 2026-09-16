package com.unla.gestionUsuario.exceptions;

public class UserException extends Exception {
	private static final long serialVersionUID = -8712610664365126958L;
	private final Type tipo;
	
	public enum Type {
        USER_NOT_FOUND("El usuario no fue encontrado."),
        INVALID_PASSWORD("La contraseña es incorrecta."),
        BLOCK_USER("El usuario está bloqueado.");
		
		private final String mensaje;
		
		Type(String mensaje) {
            this.mensaje = mensaje;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
	
	
	public UserException(Type tipo) {
        super(tipo.getMensaje());
        this.tipo = tipo;
    }

    public Type getTipo() {
        return tipo;
    }
    
    public static UserException of(Type tipo) {
        return new UserException(tipo);
    }
	
}
