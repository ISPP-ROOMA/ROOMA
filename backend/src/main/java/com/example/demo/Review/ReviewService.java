package com.example.demo.Review;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

import jakarta.transaction.Transactional;

@Service
public class ReviewService {

    private final ReviewRepositoriy reviewRepositoriy;
    private final ApartmentMemberService apartmentMemberService;
    private final UserService userService;

    @Autowired
    public ReviewService(ReviewRepositoriy reviewRepositoriy, ApartmentMemberService apartmentMemberService, UserService userService) {
        this.reviewRepositoriy = reviewRepositoriy;
        this.apartmentMemberService = apartmentMemberService;
        this.userService = userService;
    }

    public Review findById(Integer id) {
        return reviewRepositoriy.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    @Transactional
    public Review save(Review review) {
        return reviewRepositoriy.save(review);
    }

    public Review makeReviewByLandlord(Integer reviewedUserId, Integer apartmentId, String content, Integer rating) {
        String email= userService.findCurrentUser();
        UserEntity reviewerUser = userService.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Integer reviewerUserId = reviewerUser.getId();
        if(reviewRepositoriy.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(reviewerUserId, reviewedUserId, apartmentId).isPresent()) {
            throw new ResourceNotFoundException("Review already exists for the given reviewer, reviewed user and apartment");
        }

        Review review = new Review();
        review.setRating(rating);
        review.setComment(content);
        review.setPublished(false);
        review.setReviewDate(LocalDateTime.now());
    
        apartmentMemberService.checkUserIsLandlord(apartmentId, reviewerUserId);
        ApartmentMemberEntity reviewMember = apartmentMemberService.findLandlordByApartmentId(apartmentId);
        review.setReviewMember(reviewMember);

        ApartmentMemberEntity reviewedMember = apartmentMemberService.findLastMembershipByUserId(reviewedUserId);
        if(reviewedMember.getApartment().getId() != apartmentId) {
             throw new ResourceNotFoundException("Reviewed user is not a member of the apartment"); 
        }
        review.setReviewedMember(reviewedMember);
        
        return reviewRepositoriy.save(review);
    }
    
    public Review findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(Integer reviewerUserId, Integer reviewedUserId, Integer apartmentId) {
        return reviewRepositoriy.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(reviewerUserId, reviewedUserId, apartmentId).orElseThrow(() -> new ResourceNotFoundException("Review not found for the given reviewer, reviewed user and apartment"));
    }

}
