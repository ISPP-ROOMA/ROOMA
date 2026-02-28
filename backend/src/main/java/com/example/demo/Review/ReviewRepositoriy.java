package com.example.demo.Review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepositoriy extends JpaRepository<Review, Integer> {

    @Query("SELECT r FROM Review r WHERE r.reviewMember.user.id = :userId AND r.published = true")
    List<Review> findMadeReviewsByUserIdAndPublished(Integer userId);

    @Query("SELECT r FROM Review r WHERE r.reviewedMember.user.id = :userId AND r.published = true")
    List<Review> findReceivedReviewsByUserIdAndPublished(Integer userId);

    @Query("SELECT r FROM Review r WHERE r.published = false AND r.reviewDate <= CURRENT_DATE - 7")
    List<Review> findOldUnpublishedReviewsByUserId();

    @Query("SELECT r FROM Review r WHERE r.reviewMember.user.id = :userId AND r.reviewMember.apartment.id = :apartmentId AND r.published = true")
    List<Review> findMadeReviewsByUserIdAndApartmentId(Integer userId, Integer apartmentId);

    @Query("SELECT r FROM Review r WHERE r.reviewedMember.user.id = :userId AND r.reviewedMember.apartment.id = :apartmentId AND r.published = true")
    List<Review> findReceivedReviewsByUserIdAndApartmentId(Integer userId, Integer apartmentId);

    @Query("SELECT r FROM Review r WHERE r.reviewMember.user.id = :reviewerUserId AND r.reviewedMember.user.id = :reviewedUserId AND r.reviewMember.apartment.id = :apartmentId")
    Optional<Review> findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(Integer reviewerUserId, Integer reviewedUserId, Integer apartmentId);


    
}
