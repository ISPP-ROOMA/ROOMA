package com.example.demo.MemberApartment;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserRepository;
import com.example.demo.User.UserService;

@Service
public class ApartmentMemberService {

    private final ApartmentMemberRepository apartmentMemberRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;
    private final UserRepository userRepository;
    public ApartmentMemberService(ApartmentMemberRepository apartmentMemberRepository,
                                 ApartmentService apartmentService,
                                 UserService userService,
                                 UserRepository userRepository) {
        this.apartmentMemberRepository = apartmentMemberRepository;
        this.apartmentService = apartmentService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Transactional 
    public ApartmentMemberEntity addMember(Integer apartmentId, Integer userId, LocalDate joinDate) throws BadRequestException {
        if (joinDate != null && joinDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Join date cannot be in the past");
        }
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        if(userId == null || userService.findById(userId) == null) {
            throw new ResourceNotFoundException("User not found");
        }
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        UserEntity user = userService.findById(userId);

        if (apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, userId)) {
            throw new BadRequestException("User already belongs to this apartment");
        }
        checkUserIsCurrentMemberInOtherApartment(userId);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setApartment(apartment);
        member.setUser(user);
        member.setJoinDate(joinDate != null ? joinDate : LocalDate.now());
        member.setRole(user.getRole() == Role.LANDLORD ? MemberRole.HOMEBODY : MemberRole.RENTER);

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
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        UserEntity currentUser = userService.findByEmail(userService.findCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        boolean isOwner = apartment.getUser() != null && 
                          apartment.getUser().getId().equals(currentUser.getId());

        List<ApartmentMemberEntity> members = apartmentMemberRepository.findByApartmentId(apartmentId);

        boolean isMember = members.stream()
                .anyMatch(m -> m.getUser() != null && m.getUser().getId().equals(currentUser.getId()));

        boolean isAdmin = currentUser.getRole().equals(Role.ADMIN);

        if (!isOwner && !isMember && !isAdmin) {
            throw new BadRequestException("User is not the owner not a member of this apartment");
        }

        if (members.isEmpty()) {
            throw new ResourceNotFoundException("No members found in the apartment");
        }

        return members;
    }

    @Transactional(readOnly = true)
    public List<ApartmentMemberEntity> findCurrentMembers(Integer apartmentId) {
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        List<ApartmentMemberEntity> members = apartmentMemberRepository.findByApartmentIdAndEndDateIsNull(apartment.getId());
        return members;
    }


    @Transactional
    public ApartmentMemberEntity updateRole(Integer apartmentId, Integer memberId, MemberRole role) {
        ApartmentMemberEntity member = apartmentMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        if (!member.getApartment().getId().equals(apartmentId)) {
            throw new ResourceNotFoundException("Member not found in the apartment");
        }

        member.setRole(role);
        return apartmentMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Integer apartmentId, Integer memberId) {
        ApartmentMemberEntity member = apartmentMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        if (!member.getApartment().getId().equals(apartmentId)) {
            throw new ResourceNotFoundException("Member not found in the apartment");
        }

        apartmentMemberRepository.delete(member);
    }

    public List<ApartmentMemberEntity> findActiveApartmentMembers(Integer userId) {
        if(userId == null || userService.findById(userId) == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return apartmentMemberRepository.findActiveApartmentMembers(userId);
    }

    public List<ApartmentMemberEntity> findCurrentTenantsByApartmentId(Integer apartmentId) {
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
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

    public void checkUserIsTenant(Integer apartmentId, Integer userId) {
        List<ApartmentMemberEntity> tenants = findCurrentTenantsByApartmentId(apartmentId);
        boolean isTenant = tenants.stream()
                .anyMatch(m -> m.getUser() != null && m.getUser().getId().equals(userId));
        if (!isTenant) {
            throw new BadRequestException("User is not a tenant of this apartment");
        }
    }
    public void checkUserIsLastMemberInApartment(Integer apartmentId, Integer userId) {
        ApartmentMemberEntity member = findByUserIdAndApartmentId(userId, apartmentId);
        LocalDate cutoffDate = LocalDate.now().minusDays(30);
        if (member.getEndDate() != null && member.getEndDate().isBefore(cutoffDate)) {
            throw new BadRequestException("User is not the last member of this apartment");
        }
    }

    public List<ApartmentMemberEntity> findActiveMembershipsByUserId(Integer userId) {
        if(userId == null || userService.findById(userId) == null) {
             throw new ResourceNotFoundException("User not found");
        }
        UserEntity user = userService.findById(userId);
        Role role = user.getRole();

        List<ApartmentMemberEntity> activeMemberships = apartmentMemberRepository.findActiveMembershipsByUserId(userId);
        if (role.equals(Role.TENANT) && activeMemberships.size() > 1) {
            throw new BadRequestException("Tenants cannot be members of more than one apartment at the same time");
        }
        return activeMemberships;
    }

    public List<ApartmentMemberEntity> findOverlappingMemberships(Integer userId, Integer apartmentId, LocalDate joinDate, LocalDate getEndDate) {
        if(userId == null || userService.findById(userId) == null) {
             throw new ResourceNotFoundException("User not found");
        }
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        return apartmentMemberRepository.findOverlappingMemberships(userId, apartmentId, joinDate, getEndDate, LocalDate.now().minusDays(30));
    }

    public List<ApartmentMemberEntity> findOtherOverlappingMemberships(Integer excludeUserId, Integer apartmentId, LocalDate joinDate, LocalDate getEndDate) {
        if(excludeUserId == null || userService.findById(excludeUserId) == null) {
             throw new ResourceNotFoundException("User not found");
        }
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        return apartmentMemberRepository.findOtherOverlappingMemberships(excludeUserId, apartmentId, joinDate, getEndDate, LocalDate.now().minusDays(30));
    }

    public ApartmentMemberEntity findByUserIdAndApartmentId(Integer userId, Integer apartmentId) {
        return apartmentMemberRepository.findByUserIdAndApartmentId(userId, apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found for user in this apartment"));
    }

    public List<ApartmentMemberEntity> listMembersInternal(Integer apartmentId) {
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        return apartmentMemberRepository.findByApartmentId(apartmentId);
    }

    public List<ApartmentMemberEntity> findAllByUserId(Integer userId) {
        if(userId == null || userService.findById(userId) == null) {
             throw new ResourceNotFoundException("User not found");
        }
        return apartmentMemberRepository.findAllByUserId(userId);
    }

    public List<ApartmentMemberEntity> findPastLandlordMembershipsByUserIdAndApartmentId(Integer currentUserId, Integer apartmentId) {
        if(currentUserId != userService.findCurrentUserEntity().getId()) {
             throw new ForbiddenException("Access denied");
        }
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        return apartmentMemberRepository.findPastLandlordMembershipsByUserIdAndApartmentId(currentUserId, apartmentId, LocalDate.now().minusDays(30));
    }

    public List<ApartmentMemberEntity> findPastTenantMembershipsByUserIdAndApartmentId(Integer currentUserId, Integer apartmentId) {
        if(currentUserId != userService.findCurrentUserEntity().getId()) {
             throw new ForbiddenException("Access denied");
        }
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        return apartmentMemberRepository.findPastTenantMembershipsByUserIdAndApartmentId(currentUserId, apartmentId, LocalDate.now().minusDays(30));
    }

    public List<ApartmentEntity> findLastApartmentsByTenantIdAndApartmentId(Integer userId) {
        if(userId == null || userService.findById(userId) == null) {
             throw new ResourceNotFoundException("User not found");
        }
        return apartmentMemberRepository.findLastApartmentsByTenantIdAndApartmentId(userId, LocalDate.now().minusDays(30));
    }

    public List<ApartmentEntity> findLastApartmentsByLandlordIdAndApartmentId(Integer userId) {
        if(userId == null || userService.findById(userId) == null) {
             throw new ResourceNotFoundException("User not found");
        }

        return apartmentMemberRepository.findLastApartmentsByLandlordIdAndApartmentId(userId, LocalDate.now().minusDays(30));
    }


    @Transactional(readOnly = true)
    public boolean existsByUserIdAndRole(Integer userId, MemberRole role) {
        return apartmentMemberRepository.existsByUserIdAndRole(userId, role);
    }
}