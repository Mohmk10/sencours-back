package com.sencours.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class InstructorNotFoundException extends RuntimeException {

    public InstructorNotFoundException(Long instructorId) {
        super(String.format("Instructeur non trouvé avec l'ID : %d", instructorId));
    }

    public InstructorNotFoundException(String message) {
        super(message);
    }
}
