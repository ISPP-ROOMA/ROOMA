package com.example.demo.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> resourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value(), new Date());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> invalidPasswordException(InvalidPasswordException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(UserExistsException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> userExistsException(UserExistsException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> invalidRefreshTokenException(InvalidRefreshTokenException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(InvalidHashException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> invalidHashException(InvalidHashException ex) {
        ErrorResponse message = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), new Date());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

}
