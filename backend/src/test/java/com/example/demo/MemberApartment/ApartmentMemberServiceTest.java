package com.example.demo.MemberApartment;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserRepository;
import com.example.demo.User.UserService;

@ExtendWith(MockitoExtension.class)
public class ApartmentMemberServiceTest {
    
    private ApartmentMemberService apartmentMemberService;

    @Mock
    private ApartmentMemberRepository apartmentMemberRepository;

    @Mock
    private ApartmentService apartmentService;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        apartmentMemberService = new ApartmentMemberService(apartmentMemberRepository, apartmentService, userService);
    }
    
    @Test
    @DisplayName("addMember should add a member to the apartment")
    public void addMemberShouldAddMemberToApartment() {
        Integer apartmentId = 1;
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(Role.TENANT);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, userId)).thenReturn(false);
        when(apartmentMemberRepository.findActiveMembershipsByUserId(userId)).thenReturn(Collections.emptyList());
        when(apartmentMemberRepository.save(org.mockito.ArgumentMatchers.any(ApartmentMemberEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        ApartmentMemberEntity result = apartmentMemberService.addMember(apartmentId, userId, LocalDate.now());
        assert result.getApartment().getId().equals(apartmentId);
        assert result.getUser().getId().equals(userId);
        assert result.getRole().equals(MemberRole.RENTER);
    }

    @Test
    @DisplayName("addMember should throw BadRequestException if user is already a member of the apartment")
    public void addMemberShouldNotAddMemberIfAlreadyMember() {
        Integer apartmentId = 1;
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(Role.TENANT);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, userId)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.addMember(apartmentId, userId, LocalDate.now());
        });

        assert exception.getMessage().equals("User already belongs to this apartment");
    }

    @Test
    @DisplayName("addMember should throw BadRequestException if user has overlapping membership")
    public void addMemberShouldNotAddMemberIfOverlappingMembership() {
        Integer apartmentId = 1;
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(Role.TENANT);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, userId)).thenReturn(false);
        when(apartmentMemberRepository.findActiveMembershipsByUserId(userId)).thenReturn(Collections.singletonList(new ApartmentMemberEntity()));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.addMember(apartmentId, userId, LocalDate.now());
        });

        assert exception.getMessage().equals("User is already a member of another apartment");
    }

    @Test
    @DisplayName("addMember should throw ResourceNotFoundException if apartmentId is invalid")
    public void addMemberShouldNotAddMemberIfApartmentIdInvalid() {
        Integer apartmentId = 1;
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(Role.TENANT);

        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.addMember(apartmentId, userId, LocalDate.now());
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("addMember should throw ResourceNotFoundException if userId is invalid")
    public void addMemberShouldNotAddMemberIfUserIdInvalid() {
        Integer apartmentId = 1;
        Integer userId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        when(userService.findById(userId)).thenReturn(null);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.addMember(apartmentId, userId, LocalDate.now());
        });
        assert exception.getMessage().equals("User not found");
    }

    @Test
    @DisplayName("addMember should throw BadRequestException if join date is in the past")
    public void addMemberShouldNotAddMemberIfJoinDateInPast() {
        Integer apartmentId = 1;
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(Role.TENANT);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.addMember(apartmentId, userId, LocalDate.now().minusDays(1));
        });

        assert exception.getMessage().equals("Join date cannot be in the past");
    }

    @Test
    @DisplayName("listMembers should return list of members for the apartment")
    public void listMembersShouldReturnListOfMembers() {
        Integer apartmentId = 1;
        String currentUserEmail = "tenant@test.com";

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail(currentUserEmail);
        currentUser.setRole(Role.TENANT);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setApartment(apartment);
        member1.setUser(new UserEntity());
        member1.getUser().setId(1);
        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setApartment(apartment);
        member2.setUser(new UserEntity());
        member2.getUser().setId(2);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUser()).thenReturn(currentUserEmail);
        when(userService.findByEmail(currentUserEmail)).thenReturn(Optional.of(currentUser));
        when(apartmentMemberRepository.findByApartmentId(apartmentId)).thenReturn(java.util.Arrays.asList(member1, member2));
        List<ApartmentMemberEntity> members = apartmentMemberService.listMembers(apartmentId);
        assert members.size() == 2;
    }

    @Test
    @DisplayName("listMembers should throw ResourceNotFoundException if apartmentId is invalid")
    public void listMembersShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer apartmentId = 1;

        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.listMembers(apartmentId);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("listMembers should throw ResourceNotFoundException if userId is invalid")
    public void listMembersShouldThrowResourceNotFoundExceptionIfUserIdInvalid() {
        Integer apartmentId = 9999;
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUser()).thenReturn("nonexistent@test.com");
        when(userService.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.listMembers(apartmentId);
        });

        assert exception.getMessage().equals("Current user not found");
    }

    @Test
    @DisplayName("listMembers should throw BadRequestException if current user is not owner of the apartment")
    public void listMembersShouldThrowBadRequestExceptionIfUserNotOwner() {
        Integer apartmentId = 1;
        String currentUserEmail = "owner@test.com";

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail(currentUserEmail);
        currentUser.setRole(Role.TENANT);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUser()).thenReturn(currentUserEmail);
        when(userService.findByEmail(currentUserEmail)).thenReturn(Optional.of(currentUser));
        when(apartmentMemberRepository.findByApartmentId(apartmentId)).thenReturn(new java.util.ArrayList<>());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.listMembers(apartmentId);
        });

        assert exception.getMessage().equals("User is not the owner not a member of this apartment");
    }

    @Test
    @DisplayName("listMembers should throw BadRequestException if current user is not admin")
    public void listMembersShouldThrowBadRequestExceptionIfUserNotAdmin() {
        Integer apartmentId = 1;
        String currentUserEmail = "admin@test.com";

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail(currentUserEmail);
        currentUser.setRole(Role.TENANT);
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2);
        otherUser.setRole(Role.TENANT);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(1);
        member.setApartment(apartment);
        member.setUser(otherUser);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUser()).thenReturn(currentUserEmail);
        when(userService.findByEmail(currentUserEmail)).thenReturn(Optional.of(currentUser));
        when(apartmentMemberRepository.findByApartmentId(apartmentId)).thenReturn(List.of(member));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.listMembers(apartmentId);
        });

        assert exception.getMessage().equals("User is not the owner not a member of this apartment");
    }

    @Test
    @DisplayName("listMembers should throw ResourceNotFoundException if no members found for the apartment")
    public void listMembersShouldThrowResourceNotFoundExceptionIfNoMembersFound() {
        Integer apartmentId = 1;
        String currentUserEmail = "admin@test.com";

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        currentUser.setEmail(currentUserEmail);
        currentUser.setRole(Role.ADMIN);
        UserEntity otherUser = new UserEntity();
        otherUser.setId(2);
        otherUser.setRole(Role.TENANT);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUser()).thenReturn(currentUserEmail);
        when(userService.findByEmail(currentUserEmail)).thenReturn(Optional.of(currentUser));
        when(apartmentMemberRepository.findByApartmentId(apartmentId)).thenReturn(List.of());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.listMembers(apartmentId);
        });

        assert exception.getMessage().equals("No members found in the apartment");
    }

    @Test
    @DisplayName("findCurrentMembers should return list of current members for the apartment")
    public void findCurrentMembersShouldReturnListOfCurrentMembers() {
        Integer apartmentId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setApartment(apartment);
        member1.setUser(new UserEntity());
        member1.getUser().setId(1);
        member1.setEndDate(null);

        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setApartment(apartment);
        member2.setUser(new UserEntity());
        member2.getUser().setId(2);
        member2.setEndDate(LocalDate.now().plusDays(10));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.findByApartmentIdAndEndDateIsNull(apartmentId)).thenReturn(java.util.Arrays.asList(member1, member2));
        
        List<ApartmentMemberEntity> currentMembers = apartmentMemberService.findCurrentMembers(apartmentId);
        assert currentMembers.size() == 2;
    }

    @Test
    @DisplayName("findCurrentMembers should throw ResourceNotFound exception if apartmentId is invalid")
    public void findCurrentMembersShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer apartmentId = 1;

        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findCurrentMembers(apartmentId);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("updateMemberRole should update the role of a member in the apartment")
    public void updateMemberRoleShouldUpdateRole() {
        Integer apartmentId = 1;
        Integer userId = 1;
        MemberRole newRole = MemberRole.HOMEBODY;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(Role.TENANT);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(1);
        member.setApartment(apartment);
        member.setUser(user);
        member.setRole(MemberRole.RENTER);

        when(apartmentMemberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.save(org.mockito.ArgumentMatchers.any(ApartmentMemberEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ApartmentMemberEntity updatedMember = apartmentMemberService.updateRole(apartmentId, userId, newRole);
        assert updatedMember.getRole().equals(newRole);
    }

    @Test
    @DisplayName("updateRole should throw ResourceNotFoundException if apartmentId is invalid")
    public void updateRoleShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer apartmentId = 1;
        Integer userId = 1;
        MemberRole newRole = MemberRole.HOMEBODY;

        UserEntity currentUser = new UserEntity();
        currentUser.setId(1);
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(1);
        member.setApartment(apartment);
        member.setUser(currentUser);

        when(apartmentMemberRepository.findById(1)).thenReturn(Optional.of(member));
        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.updateRole(apartmentId, userId, newRole);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("updateRole should throw ResourceNotFoundException if memberId is invalid")
    public void updateRoleShouldThrowResourceNotFoundExceptionIfMemberIdInvalid() {
        Integer apartmentId = 1;
        Integer userId = 1;
        MemberRole newRole = MemberRole.HOMEBODY;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.updateRole(apartmentId, userId, newRole);
        });

        assert exception.getMessage().equals("Member not found");
    }

    @Test
    @DisplayName("removeMember should remove a member from the apartment")
    public void removeMemberShouldRemoveMember() {
        Integer apartmentId = 1;
        Integer memberId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity user = new UserEntity();
        user.setId(10);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(memberId);
        member.setApartment(apartment);
        member.setUser(user);

        when(apartmentMemberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        apartmentMemberService.removeMember(apartmentId, memberId);

        verify(apartmentMemberRepository).delete(member);
    }

    @Test
    @DisplayName("removeMember should throw ResourceNotFoundException if member is not in the apartment")
    public void removeMemberShouldThrowResourceNotFoundExceptionIfMemberNotInApartment() {
        Integer apartmentId = 1;
        Integer memberId = 1;

        ApartmentEntity otherApartment = new ApartmentEntity();
        otherApartment.setId(999);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(memberId);
        member.setApartment(otherApartment);

        when(apartmentMemberRepository.findById(memberId)).thenReturn(Optional.of(member));
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.removeMember(apartmentId, memberId);
        });

        assert exception.getMessage().equals("Member not found in the apartment");
    }

    @Test
    @DisplayName("removeMember should throw ResourceNotFoundException if apartmentId is invalid")
    public void removeMemberShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer apartmentId = 1;
        Integer memberId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity user = new UserEntity();
        user.setId(10);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(memberId);
        member.setApartment(apartment);
        member.setUser(user);

        when(apartmentMemberRepository.findById(memberId)).thenReturn(Optional.of(member));
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.removeMember(apartmentId, memberId);
        });
        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("removeMember should throw ResourceNotFoundException if memberId is invalid")
    public void removeMemberShouldThrowResourceNotFoundExceptionIfMemberIdInvalid() {
        Integer apartmentId = 1;
        Integer memberId = 1;

        when(apartmentMemberRepository.findById(memberId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.removeMember(apartmentId, memberId);
        });

        assert exception.getMessage().equals("Member not found");
    }

    @Test
    @DisplayName("findActiveApartmentMembers should return list of active members for the user")
    public void findActiveApartmentMembersShouldReturnListOfActiveMembers() {
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setUser(user);
        member1.setEndDate(null);

        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setUser(user);
        member2.setEndDate(LocalDate.now().plusDays(10));

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentMemberRepository.findActiveApartmentMembers(userId)).thenReturn(java.util.Arrays.asList(member1, member2));

        List<ApartmentMemberEntity> activeMembers = apartmentMemberService.findActiveApartmentMembers(userId);
        assert activeMembers.size() == 2;
    }

    @Test
    @DisplayName("findActiveApartmentMembers should throw ResourceNotFoundException if userId is invalid")
    public void findActiveApartmentMembersShouldThrowResourceNotFoundExceptionIfUserIdInvalid() {
        Integer userId = 1;

        when(userService.findById(userId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findActiveApartmentMembers(userId);
        });

        assert exception.getMessage().equals("User not found");
    }

    @Test
    @DisplayName("findCurrentTenantsByApartmentId should return list of current tenants for the apartment")
    public void findCurrentTenantsByApartmentIdShouldReturnListOfCurrentTenants() {
        Integer apartmentId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity user1 = new UserEntity();
        user1.setId(1);

        UserEntity user2 = new UserEntity();
        user2.setId(2);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setApartment(apartment);
        member1.setUser(user1);
        member1.setEndDate(null);

        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setApartment(apartment);
        member2.setUser(user2);
        member2.setEndDate(null);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.findCurrentTenantsByApartmentId(apartmentId)).thenReturn(List.of(member1, member2));

        List<ApartmentMemberEntity> currentTenants = apartmentMemberService.findCurrentTenantsByApartmentId(apartmentId);
        assert currentTenants.size() == 2;
    }

    @Test
    @DisplayName("findCurrentTenantsByApartmentId should throw ResourceNotFoundException if apartmentId is invalid")
    public void findCurrentTenantsByApartmentIdShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer apartmentId = 1;

        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findCurrentTenantsByApartmentId(apartmentId);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("findLastMembershipByUserId should return the last membership for the user")
    public void findLastMembershipByUserIdShouldReturnTheLastMembership() {
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);

        ApartmentMemberEntity lastMembership = new ApartmentMemberEntity();
        lastMembership.setId(1);
        lastMembership.setUser(user);
        lastMembership.setEndDate(LocalDate.now());

        when(apartmentMemberRepository.findLastMembershipByUserId(userId)).thenReturn(Optional.of(lastMembership));

        ApartmentMemberEntity result = apartmentMemberService.findLastMembershipByUserId(userId);
        assert result.equals(lastMembership);
    }

    @Test
    @DisplayName("findLastMembershipByUserId should throw ResourceNotFoundException if userId is invalid")
    public void findLastMembershipByUserIdShouldThrowResourceNotFoundExceptionIfUserIdInvalid() {
        Integer userId = 1;

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findLastMembershipByUserId(userId);
        });

        assert exception.getMessage().equals("No past memberships found for this user");
    }

    @Test
    @DisplayName("findLastMembershipByUserId should throw ResourceNotFoundException if no membership found for the user")
    public void findLastMembershipByUserIdShouldThrowResourceNotFoundExceptionIfNoMembershipFound() {
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findLastMembershipByUserId(userId);
        });

        assert exception.getMessage().equals("No past memberships found for this user");
    }

    @Test
    @DisplayName("checkUserIsCurrentMember should throw BadRequestException if user is not a current member of the apartment")
    public void checkUserIsCurrentMemberShouldThrowBadRequestExceptionIfUserNotCurrentMember() {
        Integer apartmentId = 1;
        Integer userId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity user = new UserEntity();
        user.setId(userId);

        when(apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, userId)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.checkUserIsCurrentMember(apartmentId, userId);
        });

        assert exception.getMessage().equals("User is not a current member of this apartment");
    }

    @Test
    @DisplayName("checkUserIsTenant should throw BadRequestException if user is not tenant of the apartment")
    public void checkUserIsTenantShouldThrowBadRequestExceptionIfUserNotTenant() {
        Integer apartmentId = 1;
        Integer userId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity user = new UserEntity();
        user.setId(userId);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(1);
        member.setApartment(apartment);
        member.setUser(user);
        member.setRole(MemberRole.HOMEBODY);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.findCurrentTenantsByApartmentId(apartmentId)).thenReturn(List.of());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.checkUserIsTenant(apartmentId, userId);
        });

        assert exception.getMessage().equals("User is not a tenant of this apartment");
    }

    @Test
    @DisplayName("checkUserIsLastMemberInApartment should throw BadRequestException if user is not the last member of the apartment")
    public void checkUserIsLastMemberInApartmentShouldThrowBadRequestExceptionIfUserNotLastMember() {
        Integer apartmentId = 1;
        Integer userId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        UserEntity user = new UserEntity();
        user.setId(userId);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(1);
        member.setApartment(apartment);
        member.setUser(user);
        member.setEndDate(LocalDate.now().minusDays(32));

        when(apartmentMemberRepository.findByUserIdAndApartmentId(userId, apartmentId)).thenReturn(Optional.of(member));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.checkUserIsLastMemberInApartment(apartmentId, userId);
        });

        assert exception.getMessage().equals("User is not the last member of this apartment");
    }

    @Test
    @DisplayName("findActiveMembershipsByUserId should return list of active memberships for the user")
    public void findActiveMembershipsByUserIdShouldReturnListOfActiveMemberships() {
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(Role.TENANT);

        ApartmentEntity apartment1 = new ApartmentEntity();
        apartment1.setId(1);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setApartment(apartment1);
        member1.setUser(user);
        member1.setEndDate(null);

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentMemberRepository.findActiveMembershipsByUserId(userId)).thenReturn(java.util.Arrays.asList(member1));

        List<ApartmentMemberEntity> activeMemberships = apartmentMemberService.findActiveMembershipsByUserId(userId);
        assert activeMemberships.size() == 1;
    }

    @Test
    @DisplayName("findActiveMembershipsByUserId should throw ResourceNotFoundException if userId is invalid")
    public void findActiveMembershipsByUserIdShouldThrowResourceNotFoundExceptionIfUserIdInvalid() {
        Integer userId = 1;

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findActiveMembershipsByUserId(userId);
        });

        assert exception.getMessage().equals("User not found");
    }

    @Test
    @DisplayName("findActiveMembershipsByUserId should throw BadRequestException if user has more than one active membership")
    public void findActiveMembershipsByUserIdShouldThrowBadRequestExceptionIfUserHasMultipleActiveMemberships() {
        Integer userId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRole(Role.TENANT);
        ApartmentEntity apartment1 = new ApartmentEntity();
        apartment1.setId(1);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setUser(user);
        member1.setApartment(apartment1);
        member1.setEndDate(null);

        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setUser(user);
        member2.setApartment(apartment1);
        member2.setEndDate(null);

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentMemberRepository.findActiveMembershipsByUserId(userId)).thenReturn(List.of(member1, member2));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            apartmentMemberService.findActiveMembershipsByUserId(userId);
        });

        assert exception.getMessage().equals("Tenants cannot be members of more than one apartment at the same time");
    }

    @Test
    @DisplayName("findOverlappingMemberships should return list of overlapping memberships for the user")
    public void findOverlappingMembershipsShouldReturnListOfOverlappingMemberships() {
        Integer userId = 1;
        Integer apartmentId = 1;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(10);

        UserEntity user = new UserEntity();
        user.setId(userId);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setUser(user);
        member1.setApartment(apartment);
        member1.setJoinDate(LocalDate.now().minusDays(5));
        member1.setEndDate(LocalDate.now().plusDays(5));

        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setUser(user);
        member2.setApartment(apartment);
        member2.setJoinDate(LocalDate.now().plusDays(5));
        member2.setEndDate(LocalDate.now().plusDays(15));

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.findOverlappingMemberships(
            org.mockito.ArgumentMatchers.eq(userId),
            org.mockito.ArgumentMatchers.eq(apartmentId),
            org.mockito.ArgumentMatchers.eq(startDate),
            org.mockito.ArgumentMatchers.eq(endDate),
            org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(java.util.Arrays.asList(member1, member2));

        List<ApartmentMemberEntity> overlappingMemberships = apartmentMemberService.findOverlappingMemberships(userId, apartmentId, startDate, endDate);
        assert overlappingMemberships.size() == 2;
    }

    @Test
    @DisplayName("findOverlappingMemberships should throw ResourceNotFoundException if userId is invalid")
    public void findOverlappingMembershipsShouldThrowResourceNotFoundExceptionIfUserIdInvalid() {
        Integer userId = 1;
        Integer apartmentId = 1;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(10);

        when(userService.findById(userId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findOverlappingMemberships(userId, apartmentId, startDate, endDate);
        });

        assert exception.getMessage().equals("User not found");
    }

    @Test
    @DisplayName("findOverlappingMemberships should throw ResourceNotFoundException if apartmentId is invalid")
    public void findOverlappingMembershipsShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer userId = 1;
        Integer apartmentId = 1;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(10);

        UserEntity user = new UserEntity();
        user.setId(userId);

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findOverlappingMemberships(userId, apartmentId, startDate, endDate);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("findOtherOverlappingMemberships should return list of overlapping memberships for the user excluding the specified apartment")
    public void findOtherOverlappingMembershipsShouldReturnListOfOverlappingMemberships() {
        Integer userId = 1;
        Integer apartmentId = 1;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(10);

        UserEntity user = new UserEntity();
        user.setId(userId);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setUser(user);
        member1.setApartment(apartment);
        member1.setJoinDate(LocalDate.now().minusDays(5));
        member1.setEndDate(LocalDate.now().plusDays(5));

        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setUser(user);
        member2.setApartment(new ApartmentEntity());
        member2.getApartment().setId(2);
        member2.setJoinDate(LocalDate.now().plusDays(5));
        member2.setEndDate(LocalDate.now().plusDays(15));

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.findOtherOverlappingMemberships(
            org.mockito.ArgumentMatchers.eq(userId),
            org.mockito.ArgumentMatchers.eq(apartmentId),
            org.mockito.ArgumentMatchers.eq(startDate),
            org.mockito.ArgumentMatchers.eq(endDate),
            org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(java.util.Arrays.asList(member2));

        List<ApartmentMemberEntity> overlappingMemberships = apartmentMemberService.findOtherOverlappingMemberships(userId, apartmentId, startDate, endDate);
        assert overlappingMemberships.size() == 1;
        assert overlappingMemberships.get(0).getApartment().getId() == 2;
    }

    @Test
    @DisplayName("findOtherOverlappingMemberships should throw ResourceNotFoundException if userId is invalid")
    public void findOtherOverlappingMembershipsShouldThrowResourceNotFoundExceptionIfUserIdInvalid() {
        Integer userId = 1;
        Integer apartmentId = 1;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(10);

        when(userService.findById(userId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findOtherOverlappingMemberships(userId, apartmentId, startDate, endDate);
        });

        assert exception.getMessage().equals("User not found");
    }

    @Test
    @DisplayName("findOtherOverlappingMemberships should throw ResourceNotFoundException if apartmentId is invalid")
    public void findOtherOverlappingMembershipsShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer userId = 1;
        Integer apartmentId = 1;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(10);

        UserEntity user = new UserEntity();
        user.setId(userId);

        when(userService.findById(userId)).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findOtherOverlappingMemberships(userId, apartmentId, startDate, endDate);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("findByUserIdAndApartmentId should return the membership for the user in the apartment")
    public void findByUserIdAndApartmentIdShouldReturnTheMembershipForTheUserInTheApartment() {
        Integer userId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setUser(user);
        member.setApartment(apartment);

        when(apartmentMemberRepository.findByUserIdAndApartmentId(userId, apartmentId)).thenReturn(Optional.of(member));

        ApartmentMemberEntity result = apartmentMemberService.findByUserIdAndApartmentId(userId, apartmentId);
        assert result != null;
        assert result.getUser().getId().equals(userId);
        assert result.getApartment().getId().equals(apartmentId);
    }

    @Test
    @DisplayName("findByUserIdAndApartmentId should throw ResourceNotFoundException if no membership found for the user in the apartment")
    public void findByUserIdAndApartmentIdShouldThrowResourceNotFoundExceptionIfNoMembershipFound() {
        Integer userId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        when(apartmentMemberRepository.findByUserIdAndApartmentId(userId, apartmentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findByUserIdAndApartmentId(userId, apartmentId);
        });

        assert exception.getMessage().equals("Membership not found for user in this apartment");
    }

    @Test
    @DisplayName("listMembersInternal should return list of members for the apartment")
    public void listMembersInternalShouldReturnListOfMembersForTheApartment() {
        Integer apartmentId = 1;

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        ApartmentMemberEntity member2 = new ApartmentMemberEntity();

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.findByApartmentId(apartmentId)).thenReturn(java.util.Arrays.asList(member1, member2));

        List<ApartmentMemberEntity> result = apartmentMemberService.listMembersInternal(apartmentId);
        assert result != null;
        assert result.size() == 2;
    }

    @Test
    @DisplayName("listMembersInternal should throw ResourceNotFoundException if apartmentId is invalid")
    public void listMembersInternalShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer apartmentId = 1;

        when(apartmentService.findById(apartmentId)).thenReturn(null);
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.listMembersInternal(apartmentId);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("findAllByUserId should return list of memberships for the user")
    public void findAllByUserIdShouldReturnListOfMembershipsForTheUser() {
        Integer userId = 1;

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        ApartmentMemberEntity member2 = new ApartmentMemberEntity();

        UserEntity user = new UserEntity();
        user.setId(userId);
        when(userService.findById(userId)).thenReturn(user);
        when(apartmentMemberRepository.findAllByUserId(userId)).thenReturn(java.util.Arrays.asList(member1, member2));

        List<ApartmentMemberEntity> result = apartmentMemberService.findAllByUserId(userId);
        assert result != null;
        assert result.size() == 2;
    }

    @Test
    @DisplayName("findAllByUserId should throw ResourceNotFoundException if userId is invalid")
    public void findAllByUserIdShouldThrowResourceNotFoundExceptionIfUserIdInvalid() {
        Integer userId = 1;

        when(userService.findById(userId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findAllByUserId(userId);
        });

        assert exception.getMessage().equals("User not found");
    }

    @Test
    @DisplayName("findPastLandlordMembershipsByUserIdAndApartmentId should return list of past landlord memberships for the user in the apartment")
    public void findPastLandlordMembershipsByUserIdAndApartmentIdShouldReturnListOfPastLandlordMemberships() {
        Integer currentUserId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(currentUserId);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setUser(user);
        member1.setApartment(apartment);
        member1.setRole(MemberRole.HOMEBODY);
        member1.setEndDate(LocalDate.now().minusDays(10));

        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setUser(user);
        member2.setApartment(apartment);
        member2.setRole(MemberRole.HOMEBODY);
        member2.setEndDate(LocalDate.now().minusDays(20));

        UserEntity loggedInUser = new UserEntity();
        loggedInUser.setId(currentUserId);
        when(userService.findCurrentUserEntity()).thenReturn(loggedInUser);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.findPastLandlordMembershipsByUserIdAndApartmentId(currentUserId, apartmentId, LocalDate.now().minusDays(30)))
            .thenReturn(List.of(member1, member2));
        
        List<ApartmentMemberEntity> result = apartmentMemberService.findPastLandlordMembershipsByUserIdAndApartmentId(currentUserId, apartmentId);
        assert result != null;
        assert result.size() == 2;
    }

    @Test
    @DisplayName("findPastLandlordMembershipsByUserIdAndApartmentId should throw ForbiddenException if currentUserId is not current user")
    public void findPastLandlordMembershipsByUserIdAndApartmentIdShouldThrowForbiddenExceptionIfCurrentUserIdIsNotCurrentUser() {
        Integer currentUserId = 2;
        Integer apartmentId = 1;

        UserEntity loggedInUser = new UserEntity();
        loggedInUser.setId(1);

        when(userService.findCurrentUserEntity()).thenReturn(loggedInUser);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            apartmentMemberService.findPastLandlordMembershipsByUserIdAndApartmentId(currentUserId, apartmentId);
        });

        assert exception.getMessage().equals("Access denied");
    }

    @Test
    @DisplayName("findPastLandlordMembershipsByUserIdAndApartmentId should throw ResourceNotFoundException if apartmentId is invalid")
    public void findPastLandlordMembershipsByUserIdAndApartmentIdShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer currentUserId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(currentUserId);

        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findPastLandlordMembershipsByUserIdAndApartmentId(currentUserId, apartmentId);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("findPastTenantMembershipsByUserIdAndApartmentId should return list of past tenant memberships for the user in the apartment")
    public void findPastTenantMembershipsByUserIdAndApartmentIdShouldReturnListOfPastTenantMemberships() {
        Integer currentUserId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(currentUserId);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setId(1);
        member1.setUser(user);
        member1.setApartment(apartment);
        member1.setRole(MemberRole.RENTER);
        member1.setEndDate(LocalDate.now().minusDays(10));

        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setId(2);
        member2.setUser(user);
        member2.setApartment(apartment);
        member2.setRole(MemberRole.RENTER);
        member2.setEndDate(LocalDate.now().minusDays(20));

        UserEntity loggedInUser = new UserEntity();
        loggedInUser.setId(currentUserId);
        when(userService.findCurrentUserEntity()).thenReturn(loggedInUser);
        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(apartmentMemberRepository.findPastTenantMembershipsByUserIdAndApartmentId(currentUserId, apartmentId, LocalDate.now().minusDays(30)))
            .thenReturn(List.of(member1, member2));

        List<ApartmentMemberEntity> result = apartmentMemberService.findPastTenantMembershipsByUserIdAndApartmentId(currentUserId, apartmentId);
        assert result != null;
        assert result.size() == 2;
    }

    @Test
    @DisplayName("findPastTenantMembershipsByUserIdAndApartmentId should throw ForbiddenException if currentUserId is not current user")
    public void findPastTenantMembershipsByUserIdAndApartmentIdShouldThrowForbiddenExceptionIfCurrentUserIdIsNotCurrentUser() {
        Integer currentUserId = 1;
        Integer apartmentId = 1;

        UserEntity loggedInUser = new UserEntity();
        loggedInUser.setId(2);
        when(userService.findCurrentUserEntity()).thenReturn(loggedInUser);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            apartmentMemberService.findPastTenantMembershipsByUserIdAndApartmentId(currentUserId, apartmentId);
        });

        assert exception.getMessage().equals("Access denied");
    }

    @Test
    @DisplayName("findPastTenantMembershipsByUserIdAndApartmentId should throw ResourceNotFoundException if apartmentId is invalid")
    public void findPastTenantMembershipsByUserIdAndApartmentIdShouldThrowResourceNotFoundExceptionIfApartmentIdInvalid() {
        Integer currentUserId = 1;
        Integer apartmentId = 1;

        UserEntity user = new UserEntity();
        user.setId(currentUserId);
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findPastTenantMembershipsByUserIdAndApartmentId(currentUserId, apartmentId);
        });

        assert exception.getMessage().equals("Apartment not found");
    }

    @Test
    @DisplayName("findLastApartmentsByTenantIdAndApartmentId should return list of last apartments for the tenant in the apartment")
    public void findLastApartmentsByTenantIdAndApartmentIdShouldReturnListOfLastApartments() {
        Integer currentUserId = 1;
        Integer apartmentId = 1;
        Integer otherApartmentId = 2;

        UserEntity user = new UserEntity();
        user.setId(currentUserId);

        ApartmentEntity apartment1 = new ApartmentEntity();
        apartment1.setId(apartmentId);
        ApartmentEntity apartment2 = new ApartmentEntity();
        apartment2.setId(otherApartmentId);

        when(userService.findById(currentUserId)).thenReturn(user);
        when(apartmentMemberRepository.findLastApartmentsByTenantIdAndApartmentId(currentUserId, LocalDate.now().minusDays(30))).thenReturn(List.of(apartment1, apartment2));
        
        List<ApartmentEntity> result = apartmentMemberService.findLastApartmentsByTenantIdAndApartmentId(currentUserId);
        assert result != null;
        assert result.size() == 2;
        assert result.get(0).getId().equals(apartmentId);
        assert result.get(1).getId().equals(otherApartmentId);
    }

    @Test
    @DisplayName("findLastApartmentsByTenantIdAndApartmentId should throw ResourceNotFoundException if current user does not exist")
    public void findLastApartmentsByTenantIdAndApartmentIdShouldThrowResourceNotFoundExceptionIfCurrentUserIdIsNotCurrentUser() {
        Integer currentUserId = 1;

        when(userService.findById(currentUserId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findLastApartmentsByTenantIdAndApartmentId(currentUserId);
        });

        assert exception.getMessage().equals("User not found");
    }

    @Test
    @DisplayName("findLastApartmentsByLandlordIdAndApartmentId should return list of last apartments for the landlord in the apartment")
    public void findLastApartmentsByLandlordIdAndApartmentIdShouldReturnListOfLastApartments() {
        Integer currentUserId = 1;
        Integer apartmentId = 1;
        Integer otherApartmentId = 2;

        UserEntity user = new UserEntity();
        user.setId(currentUserId);

        ApartmentEntity apartment1 = new ApartmentEntity();
        apartment1.setId(apartmentId);
        ApartmentEntity apartment2 = new ApartmentEntity();
        apartment2.setId(otherApartmentId);

        when(userService.findById(currentUserId)).thenReturn(user);
        when(apartmentMemberRepository.findLastApartmentsByLandlordIdAndApartmentId(currentUserId, LocalDate.now().minusDays(30))).thenReturn(List.of(apartment1, apartment2));
        
        List<ApartmentEntity> result = apartmentMemberService.findLastApartmentsByLandlordIdAndApartmentId(currentUserId);
        assert result != null;
        assert result.size() == 2;
        assert result.get(0).getId().equals(apartmentId);
        assert result.get(1).getId().equals(otherApartmentId);
    }

    @Test
    @DisplayName("findLastApartmentsByLandlordIdAndApartmentId should throw ResourceNotFoundException if current user does not exist")
    public void findLastApartmentsByLandlordIdAndApartmentIdShouldThrowResourceNotFoundExceptionIfCurrentUserIdIsNotCurrentUser() {
        Integer currentUserId = 1;

        when(userService.findById(currentUserId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            apartmentMemberService.findLastApartmentsByLandlordIdAndApartmentId(currentUserId);
        });

        assert exception.getMessage().equals("User not found");
    }

    @Test
    @DisplayName("existsByUserIdAndRole should return true if a membership exists for the user with the specified role")
    public void existsByUserIdAndRoleShouldReturnTrueIfMembershipExists() {
        Integer userId = 1;
        MemberRole role = MemberRole.RENTER;

        when(apartmentMemberRepository.existsByUserIdAndRole(userId, role)).thenReturn(true);

        boolean result = apartmentMemberService.existsByUserIdAndRole(userId, role);
        assert result == true;
    }

    @Test
    @DisplayName("existsByUserIdAndRole should return false if no membership exists for the user with the specified role")
    public void existsByUserIdAndRoleShouldReturnFalseIfNoMembershipExists() {
        Integer userId = 1;
        MemberRole role = MemberRole.RENTER;

        when(apartmentMemberRepository.existsByUserIdAndRole(userId, role)).thenReturn(false);

        boolean result = apartmentMemberService.existsByUserIdAndRole(userId, role);
        assert result == false;
    }
}
