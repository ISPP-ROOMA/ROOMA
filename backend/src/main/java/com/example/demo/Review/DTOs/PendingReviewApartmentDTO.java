package com.example.demo.Review.DTOs;

import java.util.List;

public record PendingReviewApartmentDTO(
        Integer apartmentId,
        String apartmentTitle,
        String apartmentUbication,
        List<ReviewableUserDTO> pendingUsers,
        Boolean userIsActive
) {
}
