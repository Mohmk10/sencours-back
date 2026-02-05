package com.sencours.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LessonNotFoundException extends RuntimeException {

    public LessonNotFoundException(Long lessonId) {
        super(String.format("Leçon non trouvée avec l'ID : %d", lessonId));
    }

    public LessonNotFoundException(String message) {
        super(message);
    }
}
