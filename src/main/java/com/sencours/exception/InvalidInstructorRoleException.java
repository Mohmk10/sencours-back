package com.sencours.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInstructorRoleException extends RuntimeException {

    public InvalidInstructorRoleException(Long userId) {
        super(String.format("L'utilisateur avec l'ID %d n'a pas le rôle INSTRUCTEUR", userId));
    }

    public InvalidInstructorRoleException(String message) {
        super(message);
    }
}
