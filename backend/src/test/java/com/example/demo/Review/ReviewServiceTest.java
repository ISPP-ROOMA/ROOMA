package com.example.demo.Review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.Notification.NotificationService;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@DisplayName("ReviewService Unit Tests")
public class ReviewServiceTest {

    private ReviewService reviewService;
    private ReviewRepository reviewRepository;
    private ApartmentMemberService apartmentMemberService;
    private UserService userService;
    private ApartmentService apartmentService;
    private NotificationService notificationService;
    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        apartmentMemberService = mock(ApartmentMemberService.class);
        userService = mock(UserService.class);
        apartmentService = mock(ApartmentService.class);
        notificationService = mock(NotificationService.class);
        reviewService = new ReviewService(reviewRepository, apartmentMemberService, userService, apartmentService, notificationService);
    }

    @Test
    void findById_found() {
        ReviewEntity review = review(11, 1, 2, 100);
        when(reviewRepository.findById(11)).thenReturn(Optional.of(review));

        ReviewEntity result = reviewService.findById(11);

        assertEquals(11, result.getId());
    }

    @Test
    void findById_notFound() {
        when(reviewRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.findById(999));
    }

    @Test
    void save_delegatesToRepository() {
        ReviewEntity review = review(1, 1, 2, 100);
        when(reviewRepository.save(review)).thenReturn(review);

        ReviewEntity saved = reviewService.save(review);

        assertNotNull(saved);
        verify(reviewRepository).save(review);
    }

    @Test
    void makeReviewByLandlord_success_withoutMutualPublish() {
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity tenant = user(2, Role.TENANT, "tenant@test.com");
        ApartmentEntity apartment = apartment(100);
        ApartmentMemberEntity tenantMembership = membership(tenant, apartment, LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));

        when(userService.findCurrentUser()).thenReturn("landlord@test.com");
        when(userService.findByEmail("landlord@test.com")).thenReturn(Optional.of(landlord));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100)).thenReturn(Optional.empty());
        when(apartmentMemberService.findLastMembershipByUserId(2)).thenReturn(tenantMembership);
        when(apartmentService.findById(100)).thenReturn(apartment);
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(i -> i.getArgument(0));

        ReviewEntity created = reviewService.makeReviewByLandlord(2, 100, "Buen inquilino", 5);

        assertEquals(5, created.getRating());
        assertEquals("Buen inquilino", created.getComment());
        assertFalse(created.getPublished());
        verify(apartmentService).checkUserIsLandlord(100, 1);
    }

    @Test
    void makeReviewByLandlord_success_withMutualPublish() {
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity tenant = user(2, Role.TENANT, "tenant@test.com");
        ApartmentEntity apartment = apartment(100);
        ApartmentMemberEntity tenantMembership = membership(tenant, apartment, LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));

        ReviewEntity reverse = review(21, 2, 1, 100);
        reverse.setPublished(false);

        when(userService.findCurrentUser()).thenReturn("landlord@test.com");
        when(userService.findByEmail("landlord@test.com")).thenReturn(Optional.of(landlord));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100)).thenReturn(Optional.empty());
        when(apartmentMemberService.findLastMembershipByUserId(2)).thenReturn(tenantMembership);
        when(apartmentService.findById(100)).thenReturn(apartment);
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(2, 1, 100)).thenReturn(Optional.of(reverse));

        ReviewEntity created = reviewService.makeReviewByLandlord(2, 100, "Buen inquilino", 5);

        assertTrue(created.getPublished());
        assertTrue(reverse.getPublished());
        verify(reviewRepository, times(3)).save(any(ReviewEntity.class));
    }

    @Test
    void makeReviewByLandlord_reviewingSelf_throws() {
        UserEntity landlord = user(7, Role.LANDLORD, "landlord@test.com");
        when(userService.findCurrentUser()).thenReturn("landlord@test.com");
        when(userService.findByEmail("landlord@test.com")).thenReturn(Optional.of(landlord));

        assertThrows(BadRequestException.class, () -> reviewService.makeReviewByLandlord(7, 100, "x", 5));
    }

    @Test
    void makeReviewByLandlord_duplicateReview_throws() {
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        when(userService.findCurrentUser()).thenReturn("landlord@test.com");
        when(userService.findByEmail("landlord@test.com")).thenReturn(Optional.of(landlord));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100))
                .thenReturn(Optional.of(review(9, 1, 2, 100)));

        assertThrows(BadRequestException.class, () -> reviewService.makeReviewByLandlord(2, 100, "x", 5));
    }

    @Test
    void makeReviewByLandlord_wrongApartment_throws() {
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity tenant = user(2, Role.TENANT, "tenant@test.com");
        ApartmentMemberEntity tenantMembership = membership(tenant, apartment(200), LocalDate.now().minusDays(5), LocalDate.now().minusDays(1));

        when(userService.findCurrentUser()).thenReturn("landlord@test.com");
        when(userService.findByEmail("landlord@test.com")).thenReturn(Optional.of(landlord));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100)).thenReturn(Optional.empty());
        when(apartmentMemberService.findLastMembershipByUserId(2)).thenReturn(tenantMembership);

        assertThrows(BadRequestException.class, () -> reviewService.makeReviewByLandlord(2, 100, "x", 5));
    }

    @Test
    void makeReviewByLandlord_tenantLeftLongAgo_throws() {
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity tenant = user(2, Role.TENANT, "tenant@test.com");
        ApartmentEntity apartment = apartment(100);
        ApartmentMemberEntity tenantMembership = membership(tenant, apartment, LocalDate.now().minusDays(60), LocalDate.now().minusDays(40));

        when(userService.findCurrentUser()).thenReturn("landlord@test.com");
        when(userService.findByEmail("landlord@test.com")).thenReturn(Optional.of(landlord));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100)).thenReturn(Optional.empty());
        when(apartmentMemberService.findLastMembershipByUserId(2)).thenReturn(tenantMembership);

        assertThrows(BadRequestException.class, () -> reviewService.makeReviewByLandlord(2, 100, "x", 5));
    }

    @Test
    void findReviewByPair_found() {
        ReviewEntity review = review(10, 1, 2, 100);
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100)).thenReturn(Optional.of(review));

        ReviewEntity result = reviewService.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100);

        assertEquals(10, result.getId());
    }

    @Test
    void findReviewByPair_notFound() {
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100));
    }

    @Test
    void makeReviewByTenant_success_forLandlord() {
        UserEntity tenant = user(3, Role.TENANT, "tenant@test.com");
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        ApartmentEntity apartment = apartment(100);
        ApartmentMemberEntity tenantMembership = membership(tenant, apartment, LocalDate.now().minusDays(10), null);

        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(tenant));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 1, 100)).thenReturn(Optional.empty());
        when(apartmentMemberService.findByUserIdAndApartmentId(3, 100)).thenReturn(tenantMembership);
        when(apartmentService.findLandlordByApartmentId(100)).thenReturn(landlord);
        when(apartmentService.findById(100)).thenReturn(apartment);
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 3, 100)).thenReturn(Optional.empty());

        ReviewEntity result = reviewService.makeReviewByTenant(1, 100, "Buen casero", 4);

        assertEquals(4, result.getRating());
        assertEquals(1, result.getReviewedMember().getId());
    }

    @Test
    void makeReviewByTenant_reviewingSelf_throws() {
        UserEntity tenant = user(3, Role.TENANT, "tenant@test.com");
        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(tenant));

        assertThrows(BadRequestException.class, () -> reviewService.makeReviewByTenant(3, 100, "x", 3));
    }

    @Test
    void makeReviewByTenant_duplicateReview_throws() {
        UserEntity tenant = user(3, Role.TENANT, "tenant@test.com");
        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(tenant));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 1, 100))
                .thenReturn(Optional.of(review(44, 3, 1, 100)));

        assertThrows(BadRequestException.class, () -> reviewService.makeReviewByTenant(1, 100, "x", 3));
    }

    @Test
    void makeReviewByTenant_nonFlatmateAndNotLandlord_throws() {
        UserEntity tenant = user(3, Role.TENANT, "tenant@test.com");
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        ApartmentEntity apartment = apartment(100);
        ApartmentMemberEntity tenantMembership = membership(tenant, apartment, LocalDate.now().minusDays(10), null);

        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(tenant));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 9, 100)).thenReturn(Optional.empty());
        when(apartmentMemberService.findByUserIdAndApartmentId(3, 100)).thenReturn(tenantMembership);
        when(apartmentService.findLandlordByApartmentId(100)).thenReturn(landlord);
        when(apartmentMemberService.findOtherOverlappingMemberships(3, 100, tenantMembership.getJoinDate(), LocalDate.now()))
                .thenReturn(List.of(membership(user(8, Role.TENANT, "t8@test.com"), apartment, LocalDate.now().minusDays(5), null)));

        assertThrows(BadRequestException.class, () -> reviewService.makeReviewByTenant(9, 100, "x", 3));
    }

    @Test
    void makeReviewByTenant_flatmatePath_success() {
        UserEntity tenant = user(3, Role.TENANT, "tenant@test.com");
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity flatmate = user(9, Role.TENANT, "flatmate@test.com");
        ApartmentEntity apartment = apartment(100);
        ApartmentMemberEntity tenantMembership = membership(tenant, apartment, LocalDate.now().minusDays(10), null);
        ApartmentMemberEntity flatmateMembership = membership(flatmate, apartment, LocalDate.now().minusDays(9), null);

        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(tenant));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 9, 100)).thenReturn(Optional.empty());
        when(apartmentMemberService.findByUserIdAndApartmentId(3, 100)).thenReturn(tenantMembership);
        when(apartmentService.findLandlordByApartmentId(100)).thenReturn(landlord);
        when(apartmentMemberService.findOtherOverlappingMemberships(3, 100, tenantMembership.getJoinDate(), LocalDate.now()))
                .thenReturn(List.of(flatmateMembership));
        when(apartmentMemberService.findByUserIdAndApartmentId(9, 100)).thenReturn(flatmateMembership);
        when(apartmentService.findById(100)).thenReturn(apartment);
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(9, 3, 100)).thenReturn(Optional.empty());

        ReviewEntity created = reviewService.makeReviewByTenant(9, 100, "Buen compi", 4);

        assertEquals(9, created.getReviewedMember().getId());
        assertEquals("Buen compi", created.getComment());
    }

    @Test
    void findMadeAndReceivedDelegates() {
        UserEntity current = user(5, Role.TENANT, "u5@test.com");
        when(userService.getUserProfile()).thenReturn(current);
        when(reviewRepository.findMadeReviewsByUserIdAndPublished(5)).thenReturn(List.of(review(1, 5, 1, 100)));
        when(reviewRepository.findReceivedReviewsByUserIdAndPublished(5)).thenReturn(List.of(review(2, 1, 5, 100)));
        when(reviewRepository.findMadeReviewsByUserIdAndApartmentId(5, 100)).thenReturn(List.of(review(3, 5, 1, 100)));
        when(reviewRepository.findReceivedReviewsByUserIdAndApartmentId(5, 100)).thenReturn(List.of(review(4, 1, 5, 100)));

        assertEquals(1, reviewService.findMadeReviewsByUserId().size());
        assertEquals(1, reviewService.findReceivedReviewsByUserId().size());
        assertEquals(1, reviewService.findMadeReviewsByUserIdAndApartmentId(100).size());
        assertEquals(1, reviewService.findReceivedReviewsByUserIdAndApartmentId(100).size());
    }

    @Test
    void publishOldReviews_publishesAll() {
        ReviewEntity r1 = review(1, 1, 2, 100);
        ReviewEntity r2 = review(2, 1, 3, 100);
        r1.setPublished(false);
        r2.setPublished(false);
        when(reviewRepository.findOldUnpublishedReviews(any(LocalDateTime.class))).thenReturn(List.of(r1, r2));

        reviewService.publishOldReviews();

        assertTrue(r1.getPublished());
        assertTrue(r2.getPublished());
        verify(reviewRepository, times(2)).save(any(ReviewEntity.class));
    }

    @Test
    void findAllMembershipsByUserId_delegates() {
        when(apartmentMemberService.findAllByUserId(10)).thenReturn(List.of());

        List<ApartmentMemberEntity> memberships = reviewService.findAllMembershipsByUserId(10);

        assertNotNull(memberships);
    }

    @Test
    void respondToReview_success() {
        UserEntity reviewed = user(2, Role.TENANT, "reviewed@test.com");
        ReviewEntity review = review(1, 1, 2, 100);
        review.setPublished(true);
        review.setReviewedMember(reviewed);

        when(userService.getUserProfile()).thenReturn(reviewed);
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);

        ReviewEntity updated = reviewService.respondToReview(1, "Gracias");

        assertEquals("Gracias", updated.getResponse());
    }

    @Test
    void respondToReview_notReviewedUser_throws() {
        UserEntity current = user(3, Role.TENANT, "other@test.com");
        ReviewEntity review = review(1, 1, 2, 100);
        review.setPublished(true);

        when(userService.getUserProfile()).thenReturn(current);
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class, () -> reviewService.respondToReview(1, "x"));
    }

    @Test
    void respondToReview_unpublished_throws() {
        UserEntity reviewed = user(2, Role.TENANT, "reviewed@test.com");
        ReviewEntity review = review(1, 1, 2, 100);
        review.setPublished(false);

        when(userService.getUserProfile()).thenReturn(reviewed);
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class, () -> reviewService.respondToReview(1, "x"));
    }

    @Test
    void respondToReview_alreadyHasResponse_throws() {
        UserEntity reviewed = user(2, Role.TENANT, "reviewed@test.com");
        ReviewEntity review = review(1, 1, 2, 100);
        review.setPublished(true);
        review.setResponse("ya respondida");

        when(userService.getUserProfile()).thenReturn(reviewed);
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class, () -> reviewService.respondToReview(1, "x"));
    }

    @Test
    void getReviewableUsers_filtersCurrentAndAlreadyReviewed() {
        UserEntity current = user(3, Role.TENANT, "tenant@test.com");
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity flatmate = user(9, Role.TENANT, "flatmate@test.com");
        ApartmentEntity apartment = apartment(100);
        ApartmentMemberEntity currentMembership = membership(current, apartment, LocalDate.now().minusDays(10), null);
        ApartmentMemberEntity flatmateMembership = membership(flatmate, apartment, LocalDate.now().minusDays(8), null);

        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(current));
        when(apartmentService.findLandlordByApartmentId(100)).thenReturn(landlord);
        when(apartmentMemberService.listMembersInternal(100)).thenReturn(List.of(
                currentMembership,
                flatmateMembership
        ));
        when(apartmentMemberService.findByUserIdAndApartmentId(3, 100)).thenReturn(currentMembership);
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 1, 100)).thenReturn(Optional.empty());
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 9, 100)).thenReturn(Optional.of(review(99, 3, 9, 100)));

        List<UserEntity> result = reviewService.getReviewableUsers(100);

        // Active tenant cannot review landlord and already reviewed the active flatmate
        assertEquals(0, result.size());
    }

    @Test
    void getPendingReviewApartments_landlordPath() {
        UserEntity currentLandlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity formerTenant = user(2, Role.TENANT, "tenant@test.com");
        ApartmentEntity apartment = apartment(100);

        when(userService.findCurrentUser()).thenReturn("landlord@test.com");
        when(userService.findByEmail("landlord@test.com")).thenReturn(Optional.of(currentLandlord));
        when(apartmentMemberService.findLastApartmentsByLandlordIdAndApartmentId(1)).thenReturn(List.of(apartment));
        when(apartmentService.findLandlordByApartmentId(100)).thenReturn(currentLandlord);
        when(apartmentMemberService.listMembersInternal(100))
            .thenReturn(List.of(membership(formerTenant, apartment, LocalDate.now().minusDays(20), LocalDate.now().minusDays(2))));
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 2, 100)).thenReturn(Optional.empty());

        List<ReviewService.PendingReviewApartment> result = reviewService.getPendingReviewApartments();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).pendingUsers().size());
        assertFalse(result.get(0).userIsActive());
    }

    @Test
    void getPendingReviewApartments_tenantInactivePath() {
        UserEntity currentTenant = user(3, Role.TENANT, "tenant@test.com");
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity currentFlatmate = user(9, Role.TENANT, "flat@test.com");
        ApartmentEntity apartment = apartment(100);

        ApartmentMemberEntity currentMembership = membership(currentTenant, apartment, LocalDate.now().minusDays(20), LocalDate.now().minusDays(1));
        ApartmentMemberEntity flatmateMembership = membership(currentFlatmate, apartment, LocalDate.now().minusDays(7), null);

        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(currentTenant));
        when(apartmentMemberService.findLastApartmentsByTenantIdAndApartmentId(3)).thenReturn(List.of(apartment));
        when(apartmentService.findLandlordByApartmentId(100)).thenReturn(landlord);
        when(apartmentMemberService.listMembersInternal(100)).thenReturn(List.of(currentMembership, flatmateMembership));
        when(apartmentMemberService.findByUserIdAndApartmentId(3, 100)).thenReturn(currentMembership);

        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 1, 100)).thenReturn(Optional.empty());
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 9, 100)).thenReturn(Optional.empty());
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 3, 100)).thenReturn(Optional.empty());
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(9, 3, 100)).thenReturn(Optional.empty());

        List<ReviewService.PendingReviewApartment> result = reviewService.getPendingReviewApartments();

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).pendingUsers().size());
        assertFalse(result.get(0).userIsActive());
    }

    @Test
    void getPendingReviewApartments_tenantActivePath() {
        UserEntity currentTenant = user(3, Role.TENANT, "tenant@test.com");
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity pastFlatmate = user(8, Role.TENANT, "past@test.com");
        ApartmentEntity apartment = apartment(100);

        ApartmentMemberEntity currentMembership = membership(currentTenant, apartment, LocalDate.now().minusDays(20), null);
        ApartmentMemberEntity pastMembership = membership(pastFlatmate, apartment, LocalDate.now().minusDays(30), LocalDate.now().minusDays(5));

        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(currentTenant));
        when(apartmentMemberService.findLastApartmentsByTenantIdAndApartmentId(3)).thenReturn(List.of(apartment));
        when(apartmentService.findLandlordByApartmentId(100)).thenReturn(landlord);
        when(apartmentMemberService.listMembersInternal(100)).thenReturn(List.of(currentMembership, pastMembership));
        when(apartmentMemberService.findByUserIdAndApartmentId(3, 100)).thenReturn(currentMembership);
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 8, 100)).thenReturn(Optional.empty());
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(8, 3, 100)).thenReturn(Optional.empty());
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 1, 100)).thenReturn(Optional.empty());
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(1, 3, 100)).thenReturn(Optional.empty());

        List<ReviewService.PendingReviewApartment> result = reviewService.getPendingReviewApartments();

        assertEquals(1, result.size());
        // Active tenant can only review the past flatmate, not the landlord
        assertEquals(1, result.get(0).pendingUsers().size());
        assertEquals(8, result.get(0).pendingUsers().get(0).user().getId());
        assertTrue(result.get(0).userIsActive());
    }

    @Test
    void getReviewableUsers_tenantInactiveCanReviewLandlord() {
        UserEntity inactiveTenant = user(3, Role.TENANT, "tenant@test.com");
        UserEntity landlord = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity flatmate = user(9, Role.TENANT, "flatmate@test.com");
        ApartmentEntity apartment = apartment(100);
        ApartmentMemberEntity inactiveMembership = membership(inactiveTenant, apartment, LocalDate.now().minusDays(20), LocalDate.now().minusDays(1));
        ApartmentMemberEntity flatmateMembership = membership(flatmate, apartment, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));

        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(inactiveTenant));
        when(apartmentService.findLandlordByApartmentId(100)).thenReturn(landlord);
        when(apartmentMemberService.listMembersInternal(100)).thenReturn(List.of(
                inactiveMembership,
                flatmateMembership
        ));
        when(apartmentMemberService.findByUserIdAndApartmentId(3, 100)).thenReturn(inactiveMembership);
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 1, 100)).thenReturn(Optional.empty());
        when(reviewRepository.findReviewsByReviewerUserIdAndReviewedUserIdAndApartmentId(3, 9, 100)).thenReturn(Optional.of(review(99, 3, 9, 100)));

        List<UserEntity> result = reviewService.getReviewableUsers(100);

        // Inactive tenant CAN review landlord (contract ended)
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    void makeReviewByTenant_neverSavesWhenInvalid() {
        UserEntity tenant = user(3, Role.TENANT, "tenant@test.com");
        when(userService.findCurrentUser()).thenReturn("tenant@test.com");
        when(userService.findByEmail("tenant@test.com")).thenReturn(Optional.of(tenant));

        assertThrows(BadRequestException.class, () -> reviewService.makeReviewByTenant(3, 100, "x", 3));
        verify(reviewRepository, never()).save(any(ReviewEntity.class));
    }

    private ReviewEntity review(Integer id, Integer reviewerId, Integer reviewedId, Integer apartmentId) {
        ReviewEntity review = new ReviewEntity();
        review.setId(id);
        review.setRating(5);
        review.setComment("comentario");
        review.setPublished(false);
        review.setReviewDate(LocalDateTime.now());
        review.setReviewMember(user(reviewerId, reviewerId == 1 ? Role.LANDLORD : Role.TENANT, "u" + reviewerId + "@t.com"));
        review.setReviewedMember(user(reviewedId, reviewedId == 1 ? Role.LANDLORD : Role.TENANT, "u" + reviewedId + "@t.com"));
        review.setApartment(apartment(apartmentId));
        return review;
    }

    private UserEntity user(Integer id, Role role, String email) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setRole(role);
        u.setEmail(email);
        return u;
    }

    private ApartmentEntity apartment(Integer id) {
        ApartmentEntity a = new ApartmentEntity();
        a.setId(id);
        a.setTitle("Apt " + id);
        a.setUbication("Sevilla");
        return a;
    }

    private ApartmentMemberEntity membership(UserEntity user, ApartmentEntity apartment, LocalDate joinDate, LocalDate endDate) {
        ApartmentMemberEntity m = new ApartmentMemberEntity();
        m.setUser(user);
        m.setApartment(apartment);
        m.setJoinDate(joinDate);
        m.setEndDate(endDate);
        return m;
    }
}
