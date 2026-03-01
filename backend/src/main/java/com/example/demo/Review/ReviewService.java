package com.example.demo.Review;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

import jakarta.transaction.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ApartmentMemberService apartmentMemberService;
    private final UserService userService;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, ApartmentMemberService apartmentMemberService, UserService userService) {
        this.reviewRepository = reviewRepository;
        this.apartmentMemberService = apartmentMemberService;
        this.userService = userService;
    }

    public ReviewEntity findById(Integer id) {
        return reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    @Transactional
    public ReviewEntity save(ReviewEntity review) {
        return reviewRepository.save(review);
    }

    @Transactional
    public ReviewEntity makeReviewByLandlord(Integer reviewedUserId, Integer apartmentId, String content, Integer rating) {
        String email = userService.findCurrentUser();
        UserEntity reviewerUser = userService.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Integer reviewerUserId = reviewerUser.getId();

        apartmentMemberService.checkUserIsLandlord(apartmentId, reviewerUserId);

        if (reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(reviewerUserId, reviewedUserId, apartmentId).isPresent()) {
            throw new BadRequestException("Review already exists for the given reviewer, reviewed user and apartment");
        }

        ApartmentMemberEntity reviewedMember = apartmentMemberService.findLastMembershipByUserId(reviewedUserId);
        if (!reviewedMember.getApartment().getId().equals(apartmentId)) {
            throw new BadRequestException("Reviewed user is not a member of the apartment");
        }

        ApartmentMemberEntity reviewMember = apartmentMemberService.findLandlordByApartmentId(apartmentId);

        ReviewEntity review = new ReviewEntity();
        review.setRating(rating);
        review.setComment(content);
        review.setPublished(false);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewMember(reviewMember);
        review.setReviewedMember(reviewedMember);

        return reviewRepository.save(review);
    }
    
    public ReviewEntity findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(Integer reviewerUserId, Integer reviewedUserId, Integer apartmentId) {
        return reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(reviewerUserId, reviewedUserId, apartmentId).orElseThrow(() -> new ResourceNotFoundException("Review not found for the given reviewer, reviewed user and apartment"));
    }

    @Transactional
    public ReviewEntity makeReviewByTenant(Integer reviewedUserId, Integer apartmentId, String content, Integer rating) {
        String email = userService.findCurrentUser();
        UserEntity reviewerUser = userService.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Integer reviewerUserId = reviewerUser.getId();

        if (reviewerUserId.equals(reviewedUserId)) {
            throw new BadRequestException("You cannot review yourself");
        }

        apartmentMemberService.checkUserIsTenant(apartmentId, reviewerUserId);

        if (reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(reviewerUserId, reviewedUserId, apartmentId).isPresent()) {
            throw new BadRequestException("Review already exists for the given reviewer, reviewed user and apartment");
        }

        ApartmentMemberEntity reviewMember = apartmentMemberService.findByUserIdAndApartmentId(reviewerUserId, apartmentId);

        ApartmentMemberEntity reviewedMember = apartmentMemberService.findByUserIdAndApartmentId(reviewedUserId, apartmentId);

        ReviewEntity review = new ReviewEntity();
        review.setRating(rating);
        review.setComment(content);
        review.setPublished(false);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewMember(reviewMember);
        review.setReviewedMember(reviewedMember);

        return reviewRepository.save(review);
    }

    public List<ReviewEntity> findMadeReviewsByUserId(Integer userId) {
        return reviewRepository.findMadeReviewsByUserIdAndPublished(userId);
    }

    public List<ReviewEntity> findReceivedReviewsByUserId(Integer userId) {
        return reviewRepository.findReceivedReviewsByUserIdAndPublished(userId);
    }

    public List<ReviewEntity> findMadeReviewsByUserIdAndApartmentId(Integer userId, Integer apartmentId) {
        return reviewRepository.findMadeReviewsByUserIdAndApartmentId(userId, apartmentId);
    }

    public List<ReviewEntity> findReceivedReviewsByUserIdAndApartmentId(Integer userId, Integer apartmentId) {
        return reviewRepository.findReceivedReviewsByUserIdAndApartmentId(userId, apartmentId);
    }

}
