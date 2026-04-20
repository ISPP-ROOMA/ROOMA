package com.example.demo.Apartment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Apartment.DTOs.ApartmentHomeDTO;
import com.example.demo.ApartmentPhoto.ApartmentPhotoEntity;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberRepository;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.MemberApartment.MemberRole;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import com.example.demo.billing.BillingService;
import com.example.demo.billing.dto.BillingSummaryDTO;

@ExtendWith(MockitoExtension.class)
public class ApartmentHomeServiceTest {

    private ApartmentHomeService apartmentHomeService;

    @Mock
    private UserService userService;

    @Mock
    private ApartmentMemberRepository apartmentMemberRepository;

    @Mock
    private ApartmentMemberService apartmentMemberService;

    @Mock
    private ApartmentPhotoService apartmentPhotoService;

    @Mock
    private BillingService billingService;

    @BeforeEach
    public void setUp() {
        apartmentHomeService = new ApartmentHomeService(
                userService,
                apartmentMemberRepository,
                apartmentMemberService,
                apartmentPhotoService,
                billingService
        );
    }

    @Test
    @DisplayName("getCurrentUserHome should aggregate apartment, roommates, photos and billing")
    public void getCurrentUserHome_ReturnsAggregatedHome() {
        UserEntity currentUser = user(1, "tenant@test.com", Role.TENANT);
        ApartmentEntity apartment = apartment(10);

        ApartmentMemberEntity currentMembership = membership(100, apartment, currentUser, null, MemberRole.RENTER, LocalDate.now().minusDays(2));
        UserEntity roommateUser = user(2, "roommate@test.com", Role.TENANT);
        ApartmentMemberEntity roommateMembership = membership(101, apartment, roommateUser, null, MemberRole.RENTER, LocalDate.now().minusDays(1));

        ApartmentPhotoEntity photo = new ApartmentPhotoEntity();
        photo.setId(50);
        photo.setUrl("https://img/apartment.jpg");
        photo.setPublicId("photo-50");
        photo.setPhoto_order(1);
        photo.setCover(true);

        BillingSummaryDTO billing = new BillingSummaryDTO(2, BigDecimal.valueOf(400), LocalDate.now().plusDays(5), "Rent");

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(apartmentMemberRepository.findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc(1))
                .thenReturn(Optional.of(currentMembership));
        when(apartmentMemberService.findCurrentMembers(10)).thenReturn(List.of(currentMembership, roommateMembership));
        when(apartmentPhotoService.findPhotosByApartmentId(10)).thenReturn(List.of(photo));
        when(billingService.getBillingSummaryForUser(1)).thenReturn(billing);

        ApartmentHomeDTO result = apartmentHomeService.getCurrentUserHome();

        assertNotNull(result);
        assertEquals(10, result.apartment().id());
        assertEquals(2, result.roommates().size());
        assertEquals(1, result.photos().size());
        assertEquals(2, result.billing().pendingDebts());
    }

    @Test
    @DisplayName("getCurrentUserHome should mark current user roommate flag")
    public void getCurrentUserHome_MarksCurrentUserInRoommates() {
        UserEntity currentUser = user(7, "tenant7@test.com", Role.TENANT);
        ApartmentEntity apartment = apartment(20);

        ApartmentMemberEntity self = membership(200, apartment, currentUser, null, MemberRole.RENTER, LocalDate.now().minusDays(2));
        ApartmentMemberEntity other = membership(201, apartment, user(8, "tenant8@test.com", Role.TENANT), null, MemberRole.RENTER, LocalDate.now().minusDays(1));

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(apartmentMemberRepository.findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc(7)).thenReturn(Optional.of(self));
        when(apartmentMemberService.findCurrentMembers(20)).thenReturn(List.of(self, other));
        when(apartmentPhotoService.findPhotosByApartmentId(20)).thenReturn(List.of());
        when(billingService.getBillingSummaryForUser(7)).thenReturn(new BillingSummaryDTO(0, BigDecimal.ZERO, null, null));

        ApartmentHomeDTO result = apartmentHomeService.getCurrentUserHome();

        long currentUserFlags = result.roommates().stream().filter(r -> r.currentUser()).count();
        assertEquals(1, currentUserFlags);
        assertEquals(7, result.roommates().stream().filter(r -> r.currentUser()).findFirst().orElseThrow().userId());
    }

    @Test
    @DisplayName("getCurrentUserHome should throw when user has no active membership")
    public void getCurrentUserHome_ThrowsWhenNoActiveMembership() {
        UserEntity currentUser = user(3, "tenant3@test.com", Role.TENANT);

        when(userService.findCurrentUserEntity()).thenReturn(currentUser);
        when(apartmentMemberRepository.findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc(3)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> apartmentHomeService.getCurrentUserHome());
    }

    private UserEntity user(Integer id, String email, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRole(role);
        return user;
    }

    private ApartmentEntity apartment(Integer id) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(id);
        apartment.setTitle("Apartment " + id);
        apartment.setDescription("desc");
        apartment.setPrice(500.0);
        apartment.setUbication("Madrid");
        apartment.setState(ApartmentState.ACTIVE);
        return apartment;
    }

    private ApartmentMemberEntity membership(Integer id,
                                             ApartmentEntity apartment,
                                             UserEntity user,
                                             LocalDate endDate,
                                             MemberRole role,
                                             LocalDate joinDate) {
        ApartmentMemberEntity membership = new ApartmentMemberEntity();
        membership.setId(id);
        membership.setApartment(apartment);
        membership.setUser(user);
        membership.setRole(role);
        membership.setJoinDate(joinDate);
        membership.setEndDate(endDate);
        return membership;
    }
}
