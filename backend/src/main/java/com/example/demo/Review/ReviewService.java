package com.example.demo.Review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Apartment.ApartmentService;
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
    private final ApartmentService apartmentService;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, ApartmentMemberService apartmentMemberService, UserService userService, ApartmentService apartmentService) {
        this.reviewRepository = reviewRepository;
        this.apartmentMemberService = apartmentMemberService;
        this.userService = userService;
        this.apartmentService = apartmentService;
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

        if(reviewerUserId.equals(reviewedUserId)) {
            throw new BadRequestException("You cannot review yourself");
        }

        apartmentService.checkUserIsLandlord(apartmentId, reviewerUserId);

        if (reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(reviewerUserId, reviewedUserId, apartmentId).isPresent()) {
            throw new BadRequestException("Review already exists for the given reviewer, reviewed user and apartment");
        }

        ApartmentMemberEntity reviewedMember = apartmentMemberService.findLastMembershipByUserId(reviewedUserId);
        if (!reviewedMember.getApartment().getId().equals(apartmentId)) {
            throw new BadRequestException("Reviewed user is not a member of the apartment");
        }

        ReviewEntity review = new ReviewEntity();
        review.setRating(rating);
        review.setComment(content);
        review.setPublished(false);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewMember(reviewerUser);
        review.setReviewedMember(reviewedMember.getUser());

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
        ApartmentMemberEntity reviewerMember = apartmentMemberService.findByUserIdAndApartmentId(reviewerUserId, apartmentId);
        ApartmentMemberEntity reviewedMember = apartmentMemberService.findByUserIdAndApartmentId(reviewedUserId, apartmentId);

        LocalDate leaveDate = reviewerMember.getLeaveDate() != null ? reviewerMember.getLeaveDate() : LocalDate.now();
        List<ApartmentMemberEntity> overlappingMemberships = apartmentMemberService.findOverlappingMemberships(reviewerUser.getId(), apartmentId, reviewerMember.getJoinDate(), leaveDate);

        boolean sharedFlat = overlappingMemberships.stream()
            .anyMatch(m -> m.getUser().getId().equals(reviewedUserId));

        boolean isLandlord = apartmentService.findLandlordByApartmentId(apartmentId).getId().equals(reviewedUserId);

        UserEntity reviewedUser = null;
        if(sharedFlat) {
            reviewedUser = reviewedMember.getUser();
        }else if(isLandlord) {
            reviewedUser = apartmentService.findLandlordByApartmentId(apartmentId);
        } else {
            throw new BadRequestException("Reviewed user is not a flatmate nor the landlord of the apartment");
        }

        ReviewEntity review = new ReviewEntity();
        review.setRating(rating);
        review.setComment(content);
        review.setPublished(false);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewMember(reviewerUser);
        review.setReviewedMember(reviewedUser);

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
