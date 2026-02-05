package com.sencours.exception;

public class EnrollmentNotFoundException extends RuntimeException {

    public EnrollmentNotFoundException(Long id) {
        super(String.format("Inscription avec l'ID %d non trouvée", id));
    }

    public EnrollmentNotFoundException(Long userId, Long courseId) {
        super(String.format("Inscription non trouvée pour l'utilisateur %d et le cours %d", userId, courseId));
    }
}
