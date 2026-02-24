package com.example.demo.MemberApartment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApartmentMemberRepository extends JpaRepository<ApartmentMemberEntity, Integer> {
    List<ApartmentMemberEntity> findByApartmentId(Integer apartmentId);

    boolean existsByApartmentIdAndUserId(Integer apartmentId, Integer userId);
}