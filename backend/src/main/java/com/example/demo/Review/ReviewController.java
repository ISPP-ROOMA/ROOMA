package com.example.demo.Review;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Review.DTOs.CreateReviewRequest;
import com.example.demo.Review.DTOs.PendingReviewApartmentDTO;
import com.example.demo.Review.DTOs.ReviewDTO;
import com.example.demo.Review.DTOs.ReviewableUserDTO;
import com.example.demo.User.UserEntity;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/landlord")
    public ResponseEntity<ReviewDTO> createReviewByLandlord(@Valid @RequestBody CreateReviewRequest request) {
        ReviewEntity review = reviewService.makeReviewByLandlord(
                request.reviewedUserId(),
                request.apartmentId(),
                request.comment(),
                request.rating()
        );
        return new ResponseEntity<>(ReviewDTO.fromEntity(review), HttpStatus.CREATED);
    }

    @PostMapping("/tenant")
    public ResponseEntity<ReviewDTO> createReviewByTenant(@Valid @RequestBody CreateReviewRequest request) {
        ReviewEntity review = reviewService.makeReviewByTenant(
                request.reviewedUserId(),
                request.apartmentId(),
                request.comment(),
                request.rating()
        );
        return new ResponseEntity<>(ReviewDTO.fromEntity(review), HttpStatus.CREATED);
    }

    @GetMapping("/made/{userId}")
    public ResponseEntity<List<ReviewDTO>> getMadeReviews(@PathVariable Integer userId) {
        List<ReviewEntity> reviews = reviewService.findMadeReviewsByUserId(userId);
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/received/{userId}")
    public ResponseEntity<List<ReviewDTO>> getReceivedReviews(@PathVariable Integer userId) {
        List<ReviewEntity> reviews = reviewService.findReceivedReviewsByUserId(userId);
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/made/{userId}/apartment/{apartmentId}")
    public ResponseEntity<List<ReviewDTO>> getMadeReviewsByApartment(
            @PathVariable Integer userId,
            @PathVariable Integer apartmentId) {
        List<ReviewEntity> reviews = reviewService.findMadeReviewsByUserIdAndApartmentId(userId, apartmentId);
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/received/{userId}/apartment/{apartmentId}")
    public ResponseEntity<List<ReviewDTO>> getReceivedReviewsByApartment(
            @PathVariable Integer userId,
            @PathVariable Integer apartmentId) {
        List<ReviewEntity> reviews = reviewService.findReceivedReviewsByUserIdAndApartmentId(userId, apartmentId);
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/reviewable/{apartmentId}")
    public ResponseEntity<List<ReviewableUserDTO>> getReviewableUsers(@PathVariable Integer apartmentId) {
        List<UserEntity> users = reviewService.getReviewableUsers(apartmentId);
        List<ReviewableUserDTO> dtos = users.stream()
                .map(u -> new ReviewableUserDTO(u.getId(), u.getEmail(), u.getRole().name()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingReviewApartmentDTO>> getPendingReviewApartments() {
        List<ReviewService.PendingReviewApartment> pending = reviewService.getPendingReviewApartments();
        List<PendingReviewApartmentDTO> dtos = pending.stream()
                .map(p -> new PendingReviewApartmentDTO(
                        p.apartment().getId(),
                        p.apartment().getTitle(),
                        p.apartment().getUbication(),
                        p.pendingUsers().stream()
                                .map(u -> new ReviewableUserDTO(u.getId(), u.getEmail(), u.getRole().name()))
                                .toList()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
