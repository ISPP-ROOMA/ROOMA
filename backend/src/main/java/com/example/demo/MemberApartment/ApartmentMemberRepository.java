package com.example.demo.MemberApartment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApartmentMemberRepository extends JpaRepository<ApartmentMemberEntity, Integer> {
    List<ApartmentMemberEntity> findByApartmentId(Integer apartmentId);
    List<ApartmentMemberEntity> findByApartmentIdAndEndDateIsNull(Integer apartmentId);

    boolean existsByApartmentIdAndUserId(Integer apartmentId, Integer userId);

    Optional<ApartmentMemberEntity> findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc(Integer userId);
}