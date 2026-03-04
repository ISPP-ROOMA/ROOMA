package com.example.demo.Review.DTOs;

public record ReviewableUserDTO(
        Integer id,
        String email,
        String role,
        Boolean hasReviewedYou,
        Boolean youReviewedThem
) {
}
