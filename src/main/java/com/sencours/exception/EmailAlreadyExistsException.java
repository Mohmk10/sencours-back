package com.sencours.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Cet email est déjà utilisé");
    }

    public EmailAlreadyExistsException(String email) {
        super("L'email " + email + " est déjà utilisé");
    }
}
