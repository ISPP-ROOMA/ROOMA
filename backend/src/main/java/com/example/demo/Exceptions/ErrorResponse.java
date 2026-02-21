package com.example.demo.Exceptions;

import java.util.Date;

public record ErrorResponse(
        String message,
        int statusCode,
        Date date
) {
}



