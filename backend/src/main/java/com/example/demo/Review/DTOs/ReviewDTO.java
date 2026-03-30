package com.example.demo.Review.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.Review.ReviewEntity;

public record ReviewDTO(
        Integer id,
        Integer rating,
        String comment,
        String response,
        Integer reviewerUserId,
    String reviewerName,
        String reviewerEmail,
        Integer reviewedUserId,
        String reviewedEmail,
        Integer apartmentId,
        Boolean published,
        LocalDateTime reviewDate
) {
    public static ReviewDTO fromEntity(ReviewEntity review) {
        return new ReviewDTO(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getResponse(),
                review.getReviewMember().getId(),
                review.getReviewMember().getName(),
                review.getReviewMember().getEmail(),
                review.getReviewedMember().getId(),
                review.getReviewedMember().getEmail(),
                review.getApartment().getId(),
                review.getPublished(),
                review.getReviewDate()
        );
    }

    public static List<ReviewDTO> fromEntityList(List<ReviewEntity> reviews) {
        return reviews.stream().map(ReviewDTO::fromEntity).toList();
    }
}
