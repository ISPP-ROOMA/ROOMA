package com.example.demo.Review;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Review.DTOs.CreateReviewRequest;
import com.example.demo.Review.DTOs.PendingReviewApartmentDTO;
import com.example.demo.Review.DTOs.RespondReviewRequest;
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

    @GetMapping("/made")
    public ResponseEntity<List<ReviewDTO>> getMadeReviews() {
        List<ReviewEntity> reviews = reviewService.findMadeReviewsByUserId();
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/received")
    public ResponseEntity<List<ReviewDTO>> getReceivedReviews() {
        List<ReviewEntity> reviews = reviewService.findReceivedReviewsByUserId();
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/received/user/{userId}")
    public ResponseEntity<Page<ReviewDTO>> getReceivedReviewsByUser(
            @PathVariable Integer userId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "5") int size) {
        Page<ReviewEntity> reviews = reviewService.findPublishedReceivedReviewsByUserId(userId, page, size);
        return ResponseEntity.ok(reviews.map(ReviewDTO::fromEntity));
    }

    @GetMapping("/received/user/{userId}/all")
    public ResponseEntity<List<ReviewDTO>> getAllReceivedReviewsByUser(@PathVariable Integer userId) {
        List<ReviewEntity> reviews = reviewService.findAllPublishedReceivedReviewsByUserId(userId);
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/made/apartment/{apartmentId}")
    public ResponseEntity<List<ReviewDTO>> getMadeReviewsByApartment(
            @PathVariable Integer apartmentId) {
        List<ReviewEntity> reviews = reviewService.findMadeReviewsByUserIdAndApartmentId(apartmentId);
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/received/apartment/{apartmentId}")
    public ResponseEntity<List<ReviewDTO>> getReceivedReviewsByApartment(
            @PathVariable Integer apartmentId) {
        List<ReviewEntity> reviews = reviewService.findReceivedReviewsByUserIdAndApartmentId(apartmentId);
        return ResponseEntity.ok(ReviewDTO.fromEntityList(reviews));
    }

    @GetMapping("/reviewable/{apartmentId}")
    public ResponseEntity<List<ReviewableUserDTO>> getReviewableUsers(@PathVariable Integer apartmentId) {
        List<UserEntity> users = reviewService.getReviewableUsers(apartmentId);
        List<ReviewableUserDTO> dtos = users.stream()
                .map(u -> new ReviewableUserDTO(u.getId(), u.getEmail(), u.getRole().name(), false, false))
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
                                .map(pu -> new ReviewableUserDTO(
                                        pu.user().getId(),
                                        pu.user().getEmail(),
                                        pu.user().getRole().name(),
                                        pu.hasReviewedYou(),
                                        pu.youReviewedThem()))
                                .toList(),
                        p.userIsActive()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{reviewId}/respond")
    public ResponseEntity<ReviewDTO> respondToReview(
            @PathVariable Integer reviewId,
            @Valid @RequestBody RespondReviewRequest request) {
        ReviewEntity review = reviewService.respondToReview(reviewId, request.response());
        return ResponseEntity.ok(ReviewDTO.fromEntity(review));
    }
}
