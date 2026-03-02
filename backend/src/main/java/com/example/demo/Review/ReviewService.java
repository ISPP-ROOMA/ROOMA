package com.example.demo.Review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Apartment.ApartmentEntity;
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

        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        ReviewEntity review = new ReviewEntity();
        review.setRating(rating);
        review.setComment(content);
        review.setPublished(false);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewMember(reviewerUser);
        review.setReviewedMember(reviewedMember.getUser());
        review.setApartment(apartment);

        ReviewEntity saved = reviewRepository.save(review);
        checkAndPublishMutualReviews(saved);
        return saved;
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

        boolean isLandlord = apartmentService.findLandlordByApartmentId(apartmentId).getId().equals(reviewedUserId);

        UserEntity reviewedUser;
        if (isLandlord) {
            reviewedUser = apartmentService.findLandlordByApartmentId(apartmentId);
        } else {
            LocalDate leaveDate = reviewerMember.getLeaveDate() != null ? reviewerMember.getLeaveDate() : LocalDate.now();
            List<ApartmentMemberEntity> overlappingMemberships = apartmentMemberService.findOtherOverlappingMemberships(
                    reviewerUser.getId(), apartmentId, reviewerMember.getJoinDate(), leaveDate);

            boolean sharedFlat = overlappingMemberships.stream()
                    .anyMatch(m -> m.getUser().getId().equals(reviewedUserId));

            if (!sharedFlat) {
                throw new BadRequestException("Reviewed user is not a flatmate nor the landlord of the apartment");
            }

            ApartmentMemberEntity reviewedMember = apartmentMemberService.findByUserIdAndApartmentId(reviewedUserId, apartmentId);
            reviewedUser = reviewedMember.getUser();
        }

        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        ReviewEntity review = new ReviewEntity();
        review.setRating(rating);
        review.setComment(content);
        review.setPublished(false);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewMember(reviewerUser);
        review.setReviewedMember(reviewedUser);
        review.setApartment(apartment);

        ReviewEntity saved = reviewRepository.save(review);
        checkAndPublishMutualReviews(saved);
        return saved;
    }

    public List<ReviewEntity> findMadeReviewsByUserId() {
        UserEntity user = userService.getUserProfile();
        return reviewRepository.findMadeReviewsByUserIdAndPublished(user.getId());
    }

    public List<ReviewEntity> findReceivedReviewsByUserId() {
        UserEntity user = userService.getUserProfile();
        return reviewRepository.findReceivedReviewsByUserIdAndPublished(user.getId());
    }

    public List<ReviewEntity> findMadeReviewsByUserIdAndApartmentId(Integer apartmentId) {
        UserEntity user = userService.getUserProfile();
        return reviewRepository.findMadeReviewsByUserIdAndApartmentId(user.getId(), apartmentId);
    }

    public List<ReviewEntity> findReceivedReviewsByUserIdAndApartmentId(Integer apartmentId) {
        UserEntity user = userService.getUserProfile();
        return reviewRepository.findReceivedReviewsByUserIdAndApartmentId(user.getId(), apartmentId);
    }

    private void checkAndPublishMutualReviews(ReviewEntity review) {
        Integer reviewerId = review.getReviewMember().getId();
        Integer reviewedId = review.getReviewedMember().getId();
        Integer apartmentId = review.getApartment().getId();

        var reverseOpt = reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(
                reviewedId, reviewerId, apartmentId);

        if (reverseOpt.isPresent()) {
            ReviewEntity reverse = reverseOpt.get();
            review.setPublished(true);
            reverse.setPublished(true);
            reviewRepository.save(review);
            reviewRepository.save(reverse);
        }
    }

    @Transactional
    public void publishOldReviews() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<ReviewEntity> oldReviews = reviewRepository.findOldUnpublishedReviews(cutoff);
        for (ReviewEntity r : oldReviews) {
            r.setPublished(true);
            reviewRepository.save(r);
        }
    }

    public List<ApartmentMemberEntity> findAllMembershipsByUserId(Integer userId) {
        return apartmentMemberService.findAllByUserId(userId);
    }

    public List<UserEntity> getReviewableUsers(Integer apartmentId) {
        String email = userService.findCurrentUser();
        UserEntity currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        Integer currentUserId = currentUser.getId();

        java.util.Set<UserEntity> candidates = new java.util.LinkedHashSet<>();

        UserEntity landlord = apartmentService.findLandlordByApartmentId(apartmentId);
        candidates.add(landlord);

        List<ApartmentMemberEntity> members = apartmentMemberService.listMembersInternal(apartmentId);
        for (ApartmentMemberEntity m : members) {
            candidates.add(m.getUser());
        }

        candidates.removeIf(u -> u.getId().equals(currentUserId));

        List<UserEntity> reviewable = new java.util.ArrayList<>();
        for (UserEntity candidate : candidates) {
            var existing = reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(
                    currentUserId, candidate.getId(), apartmentId);
            if (existing.isEmpty()) {
                reviewable.add(candidate);
            }
        }

        return reviewable;
    }

    public List<PendingReviewApartment> getPendingReviewApartments() {
        String email = userService.findCurrentUser();
        UserEntity currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        Integer currentUserId = currentUser.getId();

        Set<Integer> apartmentIds = new LinkedHashSet<>();

        List<ApartmentMemberEntity> memberships = apartmentMemberService.findAllByUserId(currentUserId);
        for (ApartmentMemberEntity m : memberships) {
            apartmentIds.add(m.getApartment().getId());
        }

        List<ApartmentEntity> ownedApartments = apartmentService.findAllByUserId(currentUserId);
        for (ApartmentEntity a : ownedApartments) {
            apartmentIds.add(a.getId());
        }

        List<PendingReviewApartment> result = new ArrayList<>();

        for (Integer aptId : apartmentIds) {
            ApartmentEntity apartment = apartmentService.findById(aptId);

            Set<UserEntity> candidates = new LinkedHashSet<>();

            try {
                UserEntity landlord = apartmentService.findLandlordByApartmentId(aptId);
                candidates.add(landlord);
            } catch (Exception ignored) {}

            List<ApartmentMemberEntity> members = apartmentMemberService.listMembersInternal(aptId);
            for (ApartmentMemberEntity m : members) {
                candidates.add(m.getUser());
            }

            candidates.removeIf(u -> u.getId().equals(currentUserId));

            List<UserEntity> pending = new ArrayList<>();
            for (UserEntity candidate : candidates) {
                var existing = reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(
                        currentUserId, candidate.getId(), aptId);
                if (existing.isEmpty()) {
                    pending.add(candidate);
                }
            }

            if (!pending.isEmpty()) {
                result.add(new PendingReviewApartment(apartment, pending));
            }
        }

        return result;
    }

    public record PendingReviewApartment(ApartmentEntity apartment, List<UserEntity> pendingUsers) {}

}
