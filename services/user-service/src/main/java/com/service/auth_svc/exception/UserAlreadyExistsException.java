package com.service.auth_svc.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends CustomException {
    public UserAlreadyExistsException(String email) {
        super("User already exists with email: " + email, HttpStatus.CONFLICT);
    }
}
