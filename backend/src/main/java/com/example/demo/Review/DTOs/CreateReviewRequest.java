package com.example.demo.Review.DTOs;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull Integer reviewedUserId,
        @NotNull Integer apartmentId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotNull @Size(max = 500) String comment
) {
}
