package com.example.demo.MemberApartment;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentRepository;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserRepository;
import com.example.demo.User.UserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
        checkUserIsOwnerOrMember(apartmentId, userId);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setApartment(apartment);
        member.setUser(user);
        member.setJoinDate(joinDate != null ? joinDate : LocalDate.now());

        return apartmentMemberRepository.save(member);
    }
    private void checkUserIsOwnerOrMember(Integer apartmentId,Integer userId) {
        ApartmentEntity apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found"));

        UserEntity currentUser = userService.findById(userId);

        boolean isOwner = apartment.getUser() != null && 
                          apartment.getUser().getId().equals(currentUser.getId());

        boolean isMember = apartmentMemberRepository.existsByApartmentIdAndUserId(apartmentId, currentUser.getId());

        if (!isOwner && !isMember) {
            throw new BadRequestException("User is not the owner nor a member of this apartment");
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
}