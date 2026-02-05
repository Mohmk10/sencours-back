package com.sencours.exception;

public class AlreadyEnrolledException extends RuntimeException {

    public AlreadyEnrolledException(Long userId, Long courseId) {
        super(String.format("L'utilisateur %d est déjà inscrit au cours %d", userId, courseId));
    }
}
