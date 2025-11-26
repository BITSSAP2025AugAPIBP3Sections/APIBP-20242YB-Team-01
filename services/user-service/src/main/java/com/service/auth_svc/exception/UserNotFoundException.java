package com.service.auth_svc.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CustomException {
    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier, HttpStatus.NOT_FOUND);
    }

    public UserNotFoundException() {
        super("User not found", HttpStatus.NOT_FOUND);
    }
}
