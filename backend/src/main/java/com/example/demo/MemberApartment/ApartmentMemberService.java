package com.example.demo.MemberApartment;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ApartmentMemberService {

    private final ApartmentMemberRepository apartmentMemberRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;

    public ApartmentMemberService(ApartmentMemberRepository apartmentMemberRepository,
                                 ApartmentService apartmentService,
                                 UserService userService) {
        this.apartmentMemberRepository = apartmentMemberRepository;
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    @Transactional
    public ApartmentMemberEntity addMember(Integer apartmentId, Integer userId, MemberRole role, LocalDate joinDate) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        UserEntity user = userService.findById(userId);

        if (apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, userId)) {
            throw new BadRequestException("User already belongs to this apartment");
        }

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setApartment(apartment);
        member.setUser(user);
        member.setRole(role);
        member.setJoinDate(joinDate != null ? joinDate : LocalDate.now());

        return apartmentMemberRepository.save(member);
    }

@Transactional(readOnly = true)
    public List<ApartmentMemberEntity> listMembers(Integer apartmentId) {

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
            throw new BadRequestException("User is not the owner nor a member of this apartment");
        }

        if (members.isEmpty()) {
            throw new ResourceNotFoundException("No members found in the apartment");
        }

        return members;
    }

    @Transactional(readOnly = true)
    public List<ApartmentMemberEntity> findCurrentMembers(Integer apartmentId) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        List<ApartmentMemberEntity> members = apartmentMemberRepository.findByApartmentIdAndEndDateIsNull(apartment.getId());
        return members;
    }


    @Transactional
    public ApartmentMemberEntity updateRole(Integer apartmentId, Integer memberId, MemberRole role) {
        ApartmentMemberEntity member = apartmentMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

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

        if (!member.getApartment().getId().equals(apartmentId)) {
            throw new ResourceNotFoundException("Member not found in the apartment");
        }

        apartmentMemberRepository.delete(member);
    }
}