package com.example.demo.MemberApartment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApartmentMemberRepository extends JpaRepository<ApartmentMemberEntity, Integer> {
    List<ApartmentMemberEntity> findByApartmentId(Integer apartmentId);

    boolean existsByApartmentIdAndUserId(Integer apartmentId, Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.apartment.id = :apartmentId AND (m.leaveDate IS NULL OR m.leaveDate > CURRENT_DATE)")
    List<ApartmentMemberEntity> findActiveApartmentMembers(Integer apartmentId);
}