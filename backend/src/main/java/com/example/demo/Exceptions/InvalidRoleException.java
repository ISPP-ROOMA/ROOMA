package com.example.demo.Exceptions;


public class InvalidRoleException extends RuntimeException {
     public InvalidRoleException(String message) {
        super(message);
    }
}
