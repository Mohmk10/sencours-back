package com.sencours.exception;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(Long id) {
        super(String.format("Avis avec l'ID %d non trouvé", id));
    }

    public ReviewNotFoundException(Long userId, Long courseId) {
        super(String.format("Avis non trouvé pour l'utilisateur %d et le cours %d", userId, courseId));
    }
}
