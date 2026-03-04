package com.example.demo.Review.DTOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RespondReviewRequest(
        @NotNull @Size(min = 1, max = 500) String response
) {
}
