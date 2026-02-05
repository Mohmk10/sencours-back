package com.sencours.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SectionNotFoundException extends RuntimeException {

    public SectionNotFoundException(Long sectionId) {
        super(String.format("Section non trouvée avec l'ID : %d", sectionId));
    }

    public SectionNotFoundException(String message) {
        super(message);
    }
}
