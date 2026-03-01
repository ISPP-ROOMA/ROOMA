package com.example.demo.MemberApartment;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentRepository;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserRepository;
import com.example.demo.User.UserService;

@Service
public class ApartmentMemberService {

    private final ApartmentMemberRepository apartmentMemberRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public ApartmentMemberService(ApartmentMemberRepository apartmentMemberRepository,
                                 ApartmentRepository apartmentRepository,
                                 UserRepository userRepository,
                                 UserService userService) {
        this.apartmentMemberRepository = apartmentMemberRepository;
        this.apartmentRepository = apartmentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public ApartmentMemberEntity addMember(Integer apartmentId, Integer userId, LocalDate joinDate) {
        ApartmentEntity apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, userId)) {
            throw new BadRequestException("User already belongs to this apartment");
        }
        checkUserIsCurrentMemberInOtherApartment(userId);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setApartment(apartment);
        member.setUser(user);
        member.setJoinDate(joinDate != null ? joinDate : LocalDate.now());

        return apartmentMemberRepository.save(member);
    }
    private void checkUserIsCurrentMemberInOtherApartment(Integer userId) {
        List<ApartmentMemberEntity> activeMemberships = findActiveMembershipsByUserId(userId);
        if (!activeMemberships.isEmpty()) {
            throw new BadRequestException("User is already a member of another apartment");
        }
    }

    @Transactional(readOnly = true)
    public List<ApartmentMemberEntity> listMembers(Integer apartmentId) {

        ApartmentEntity apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found"));

        UserEntity currentUser = userService.findByEmail(userService.findCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        boolean isOwner = apartment.getUser() != null && 
                          apartment.getUser().getId().equals(currentUser.getId());

        List<ApartmentMemberEntity> members = apartmentMemberRepository.findByApartmentId(apartmentId);

        boolean isMember = members.stream()
                .anyMatch(m -> m.getUser() != null && m.getUser().getId().equals(currentUser.getId()));

        boolean isAdmin = currentUser.getRole().equals(Role.ADMIN);

        if (!isOwner && !isMember && !isAdmin) {
            throw new BadRequestException("User is not the owner nor a member of this apartment");
        }

        if (members.isEmpty()) {
            throw new ResourceNotFoundException("No members found in the apartment");
        }

        return members;
    }

    @Transactional
    public void removeMember(Integer apartmentId, Integer memberId) {
        ApartmentMemberEntity member = apartmentMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!member.getApartment().getId().equals(apartmentId)) {
            throw new ResourceNotFoundException("Member not found in the apartment");
        }

        apartmentMemberRepository.delete(member);
    }

    public List<ApartmentMemberEntity> findActiveApartmentMembers(Integer userId) {
        return apartmentMemberRepository.findActiveApartmentMembers(userId);
    }

    public ApartmentMemberEntity findLandlordByApartmentId(Integer apartmentId) {
        return apartmentMemberRepository.findLandlordByApartmentId(apartmentId).orElseThrow(() -> new ResourceNotFoundException("Landlord not found for this apartment"));
    }

    public List<ApartmentMemberEntity> findCurrentTenantsByApartmentId(Integer apartmentId) {
        return apartmentMemberRepository.findCurrentTenantsByApartmentId(apartmentId);
    }

    public ApartmentMemberEntity findLastMembershipByUserId(Integer userId) {
        return apartmentMemberRepository.findLastMembershipByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No past memberships found for this user"));
    }

    public void checkUserIsCurrentMember(Integer apartmentId, Integer userId) {
        boolean isMember = apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, userId);
        if (!isMember) {
            throw new BadRequestException("User is not a current member of this apartment");
        }
    }

    public void checkUserIsLandlord(Integer apartmentId, Integer userId) {
        ApartmentMemberEntity landlord = findLandlordByApartmentId(apartmentId);
        if (landlord == null || landlord.getUser() == null || !landlord.getUser().getId().equals(userId)) {
            throw new BadRequestException("User is not the landlord of this apartment");
        }
    }

    public void checkUserIsTenant(Integer apartmentId, Integer userId) {
        List<ApartmentMemberEntity> tenants = findCurrentTenantsByApartmentId(apartmentId);
        boolean isTenant = tenants.stream()
                .anyMatch(m -> m.getUser() != null && m.getUser().getId().equals(userId));
        if (!isTenant) {
            throw new BadRequestException("User is not a tenant of this apartment");
        }
    }
    public void checkUserIsLastMemberInApartment(Integer apartmentId, Integer userId) {
        ApartmentMemberEntity lastMember = findLastMembershipByUserId(userId);
        if (lastMember.getApartment() == null || !lastMember.getApartment().getId().equals(apartmentId)) {
            throw new BadRequestException("User is not the last member of this apartment");
        }
    }

    public List<ApartmentMemberEntity> findActiveMembershipsByUserId(Integer userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role = user.getRole();

        List<ApartmentMemberEntity> activeMemberships = apartmentMemberRepository.findActiveMembershipsByUserId(userId);
        if (role.equals(Role.TENANT) && activeMemberships.size() > 1) {
            throw new BadRequestException("Tenants cannot be members of more than one apartment at the same time");
        }
        return activeMemberships;
    }

    public List<ApartmentMemberEntity> findOverlappingMemberships(Integer userId, Integer apartmentId, LocalDate joinDate, LocalDate leaveDate) {
        return apartmentMemberRepository.findOverlappingMemberships(userId, apartmentId, joinDate, leaveDate);
    }

    public ApartmentMemberEntity findByUserIdAndApartmentId(Integer userId, Integer apartmentId) {
        return apartmentMemberRepository.findByUserIdAndApartmentId(userId, apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found for user in this apartment"));
    }

}