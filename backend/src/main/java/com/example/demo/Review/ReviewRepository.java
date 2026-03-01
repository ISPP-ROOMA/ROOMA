package com.example.demo.Review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {

    @Query("SELECT r FROM ReviewEntity r WHERE r.reviewMember.user.id = :userId AND r.published = true")
    List<ReviewEntity> findMadeReviewsByUserIdAndPublished(Integer userId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.reviewedMember.user.id = :userId AND r.published = true")
    List<ReviewEntity> findReceivedReviewsByUserIdAndPublished(Integer userId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.published = false AND r.reviewDate <= :cutoffDate")
    List<ReviewEntity> findOldUnpublishedReviews(LocalDateTime cutoffDate);

    @Query("SELECT r FROM ReviewEntity r WHERE r.reviewMember.user.id = :userId AND r.reviewMember.apartment.id = :apartmentId AND r.published = true")
    List<ReviewEntity> findMadeReviewsByUserIdAndApartmentId(Integer userId, Integer apartmentId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.reviewedMember.user.id = :userId AND r.reviewedMember.apartment.id = :apartmentId AND r.published = true")
    List<ReviewEntity> findReceivedReviewsByUserIdAndApartmentId(Integer userId, Integer apartmentId);

    @Query("SELECT r FROM ReviewEntity r WHERE r.reviewMember.user.id = :reviewerUserId AND r.reviewedMember.user.id = :reviewedUserId AND r.reviewMember.apartment.id = :apartmentId")
    Optional<ReviewEntity> findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(Integer reviewerUserId, Integer reviewedUserId, Integer apartmentId);


    
}
