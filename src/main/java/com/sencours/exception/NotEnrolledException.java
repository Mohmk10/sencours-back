package com.sencours.exception;

public class NotEnrolledException extends RuntimeException {

    public NotEnrolledException(Long userId, Long courseId) {
        super(String.format("L'utilisateur %d n'est pas inscrit au cours %d. Vous devez être inscrit pour laisser un avis.", userId, courseId));
    }
}
