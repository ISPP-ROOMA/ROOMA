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
                review.getReviewMember().getUser().getId(),
                review.getReviewMember().getUser().getEmail(),
                review.getReviewedMember().getUser().getId(),
                review.getReviewedMember().getUser().getEmail(),
                review.getReviewMember().getApartment().getId(),
                review.getPublished(),
                review.getReviewDate()
        );
    }

    public static List<ReviewDTO> fromEntityList(List<ReviewEntity> reviews) {
        return reviews.stream().map(ReviewDTO::fromEntity).toList();
    }
}
