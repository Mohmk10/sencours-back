package com.sencours.exception;

public class UnauthorizedReviewAccessException extends RuntimeException {

    public UnauthorizedReviewAccessException() {
        super("Vous ne pouvez modifier ou supprimer que vos propres avis");
    }

    public UnauthorizedReviewAccessException(String message) {
        super(message);
    }
}
