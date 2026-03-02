package com.example.demo.MemberApartment.DTOs;

import java.time.LocalDate;
import java.util.List;

import com.example.demo.MemberApartment.ApartmentMemberEntity;

public record RoommateDTO(
        Integer memberId,
        Integer userId,
        String email,
        String profession,
        String hobbies,
        String schedule,
        String profileImageUrl,
        String memberRole,
        LocalDate joinDate,
        boolean currentUser
) {
    public static RoommateDTO fromEntity(ApartmentMemberEntity member, Integer currentUserId) {
        var user = member.getUser();
        return new RoommateDTO(
                member.getId(),
                user.getId(),
                user.getEmail(),
                user.getProfession(),
                user.getHobbies(),
                user.getSchedule(),
                user.getProfileImageUrl(),
                member.getRole().name(),
                member.getJoinDate(),
                user.getId().equals(currentUserId)
        );
    }

    public static List<RoommateDTO> fromEntityList(List<ApartmentMemberEntity> members, Integer currentUserId) {
        return members.stream()
                .map(member -> RoommateDTO.fromEntity(member, currentUserId))
                .toList();
    }
}
