package com.example.demo.MemberApartment;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentRepository;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ApartmentMemberService {

    private final ApartmentMemberRepository apartmentMemberRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    public ApartmentMemberService(ApartmentMemberRepository apartmentMemberRepository,
                                 ApartmentRepository apartmentRepository,
                                 UserRepository userRepository) {
        this.apartmentMemberRepository = apartmentMemberRepository;
        this.apartmentRepository = apartmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApartmentMemberEntity addMember(Integer apartmentId, Integer userId, MemberRole role, LocalDate joinDate) {
        ApartmentEntity apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
        if (!apartmentRepository.existsById(apartmentId)) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        return apartmentMemberRepository.findByApartmentId(apartmentId);
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