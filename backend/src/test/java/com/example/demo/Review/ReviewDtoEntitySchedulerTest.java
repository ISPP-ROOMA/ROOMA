package com.example.demo.Review;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Review.DTOs.CreateReviewRequest;
import com.example.demo.Review.DTOs.PendingReviewApartmentDTO;
import com.example.demo.Review.DTOs.RespondReviewRequest;
import com.example.demo.Review.DTOs.ReviewDTO;
import com.example.demo.Review.DTOs.ReviewableUserDTO;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;

@DisplayName("Review DTO Entity Scheduler Tests")
public class ReviewDtoEntitySchedulerTest {

    @Test
    void reviewDto_fromEntity_and_fromList() {
        ReviewEntity entity = entity(1, 5, "Muy bien");

        ReviewDTO dto = ReviewDTO.fromEntity(entity);
        List<ReviewDTO> list = ReviewDTO.fromEntityList(List.of(entity));

        assertEquals(1, dto.id());
        assertEquals(5, dto.rating());
        assertEquals("Muy bien", dto.comment());
        assertEquals(1, list.size());
    }

    @Test
    void reviewEntity_constructor_and_accessors() {
        UserEntity reviewer = user(1, Role.LANDLORD, "landlord@test.com");
        UserEntity reviewed = user(2, Role.TENANT, "tenant@test.com");
        ApartmentEntity apartment = apartment(100);
        LocalDateTime now = LocalDateTime.now();

        ReviewEntity entity = new ReviewEntity(1, 4, "Comentario", "Respuesta", reviewer, reviewed, true, now, apartment);

        assertEquals(1, entity.getId());
        assertEquals(4, entity.getRating());
        assertEquals("Comentario", entity.getComment());
        assertEquals("Respuesta", entity.getResponse());
        assertEquals(1, entity.getReviewMember().getId());
        assertEquals(2, entity.getReviewedMember().getId());
        assertTrue(entity.getPublished());
        assertEquals(now, entity.getReviewDate());
        assertEquals(100, entity.getApartment().getId());

        entity.setRating(3);
        entity.setComment("Otro");
        entity.setResponse("Otra respuesta");
        entity.setPublished(false);

        assertEquals(3, entity.getRating());
        assertEquals("Otro", entity.getComment());
        assertEquals("Otra respuesta", entity.getResponse());
        assertFalse(entity.getPublished());
    }

    @Test
    void records_cover_accessors() {
        CreateReviewRequest create = new CreateReviewRequest(2, 100, 5, "Texto");
        RespondReviewRequest respond = new RespondReviewRequest("Gracias");
        ReviewableUserDTO reviewable = new ReviewableUserDTO(9, "u@test.com", "TENANT", true, false);
        PendingReviewApartmentDTO pending = new PendingReviewApartmentDTO(100, "Apt 100", "Sevilla", List.of(reviewable));

        assertEquals(2, create.reviewedUserId());
        assertEquals(100, create.apartmentId());
        assertEquals(5, create.rating());
        assertEquals("Texto", create.comment());

        assertEquals("Gracias", respond.response());
        assertEquals(9, reviewable.id());
        assertEquals("TENANT", reviewable.role());
        assertTrue(reviewable.hasReviewedYou());
        assertFalse(reviewable.youReviewedThem());

        assertEquals(100, pending.apartmentId());
        assertEquals("Apt 100", pending.apartmentTitle());
        assertEquals(1, pending.pendingUsers().size());
    }

    @Test
    void scheduler_calls_publishOldReviews() {
        ReviewService reviewService = mock(ReviewService.class);
        ReviewScheduler scheduler = new ReviewScheduler(reviewService);

        scheduler.autoPublishOldReviews();

        verify(reviewService).publishOldReviews();
    }

    @Test
    void pendingRecords_fromService_are_accessible() {
        UserEntity user = user(5, Role.TENANT, "pending@test.com");
        ApartmentEntity apartment = apartment(10);

        ReviewService.PendingUserInfo info = new ReviewService.PendingUserInfo(user, true, false);
        ReviewService.PendingReviewApartment pending = new ReviewService.PendingReviewApartment(apartment, List.of(info));

        assertNotNull(pending.apartment());
        assertEquals(10, pending.apartment().getId());
        assertEquals(1, pending.pendingUsers().size());
        assertTrue(pending.pendingUsers().get(0).hasReviewedYou());
    }

    private ReviewEntity entity(Integer id, Integer rating, String comment) {
        ReviewEntity e = new ReviewEntity();
        e.setId(id);
        e.setRating(rating);
        e.setComment(comment);
        e.setResponse(null);
        e.setReviewMember(user(1, Role.LANDLORD, "landlord@test.com"));
        e.setReviewedMember(user(2, Role.TENANT, "tenant@test.com"));
        e.setApartment(apartment(100));
        e.setPublished(true);
        e.setReviewDate(LocalDateTime.now());
        return e;
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
}
