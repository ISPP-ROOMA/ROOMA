package com.example.demo.MemberApartment.DTOs;

import java.time.LocalDate;
import java.util.List;

import com.example.demo.MemberApartment.ApartmentMemberEntity;

public record ApartmentMemberDTO(
        Integer id,
        Integer apartmentId,
        Integer userId,
        LocalDate joinDate
) {
    public static ApartmentMemberDTO fromEntity(ApartmentMemberEntity member) {
        return new ApartmentMemberDTO(
                member.getId(),
                member.getApartment().getId(),
                member.getUser().getId(),
                member.getJoinDate()
        );
    }

    public static List<ApartmentMemberDTO> fromEntityList(List<ApartmentMemberEntity> members) {
        return members.stream().map(ApartmentMemberDTO::fromEntity).toList();
    }
}