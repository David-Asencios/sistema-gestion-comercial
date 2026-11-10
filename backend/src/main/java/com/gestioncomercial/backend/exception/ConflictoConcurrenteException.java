package com.gestioncomercial.backend.exception;

public class ConflictoConcurrenteException extends RuntimeException {
    public ConflictoConcurrenteException(String mensaje) {
        super(mensaje);
    }
}
