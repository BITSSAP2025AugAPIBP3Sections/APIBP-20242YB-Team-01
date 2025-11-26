package com.service.auth_svc.exception;

import org.springframework.http.HttpStatus;

public class InvalidRoleException extends CustomException {
    public InvalidRoleException(String role) {
        super("Invalid role: " + role + ". Allowed values: ADMIN, BUYER, SELLER", HttpStatus.BAD_REQUEST);
    }
}
