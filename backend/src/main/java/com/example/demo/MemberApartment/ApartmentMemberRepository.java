package com.example.demo.MemberApartment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApartmentMemberRepository extends JpaRepository<ApartmentMemberEntity, Integer> {
    List<ApartmentMemberEntity> findByApartmentId(Integer apartmentId);

    boolean existsByApartmentIdAndUserId(Integer apartmentId, Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.apartment.id = :apartmentId AND (m.leaveDate IS NULL OR m.leaveDate > CURRENT_DATE)")
    List<ApartmentMemberEntity> findActiveApartmentMembers(Integer apartmentId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.apartment.id = :apartmentId AND m.user.role = 'LANDLORD'")
    Optional<ApartmentMemberEntity> findLandlordByApartmentId(Integer apartmentId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.apartment.id = :apartmentId AND m.user.role = 'TENANT' AND (m.leaveDate IS NULL OR m.leaveDate > CURRENT_DATE)")
    List<ApartmentMemberEntity> findCurrentTenantsByApartmentId(Integer apartmentId);
    
    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND m.leaveDate IS NOT NULL ORDER BY m.leaveDate DESC")
    Optional<ApartmentMemberEntity> findLastMembershipByUserId(Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND (m.leaveDate IS NULL OR m.leaveDate > CURRENT_DATE) ORDER BY m.joinDate DESC")
    List<ApartmentMemberEntity> findActiveMembershipsByUserId(Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND m.apartment.id = :apartmentId AND ((m.joinDate <= :joinDate AND (m.leaveDate IS NULL OR m.leaveDate >= :joinDate)) OR (m.joinDate <= :leaveDate AND (m.leaveDate IS NULL OR m.leaveDate >= :leaveDate)) OR (m.joinDate >= :joinDate AND (m.leaveDate IS NULL OR m.leaveDate <= :leaveDate)))")
    List<ApartmentMemberEntity> findOverlappingMemberships(Integer userId, Integer apartmentId, LocalDate joinDate, LocalDate leaveDate);
}