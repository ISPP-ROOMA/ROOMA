package com.example.demo.Exceptions;

import java.util.Date;
import java.util.Map;

public record ErrorResponse(
        String message,
        int statusCode,
        Date date,
        Map<String, String> errors
) {
    public ErrorResponse(String message, int statusCode, Date date) {
        this(message, statusCode, date, null);
    }
}



