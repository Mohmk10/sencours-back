package com.sencours.exception;

public class ReviewAlreadyExistsException extends RuntimeException {

    public ReviewAlreadyExistsException(Long userId, Long courseId) {
        super(String.format("Vous avez déjà noté ce cours (utilisateur %d, cours %d)", userId, courseId));
    }
}
