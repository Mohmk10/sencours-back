package com.sencours.exception;

public class ProgressNotFoundException extends RuntimeException {

    public ProgressNotFoundException(Long enrollmentId, Long lessonId) {
        super(String.format("Progression non trouvée pour l'inscription %d et la leçon %d", enrollmentId, lessonId));
    }
}
