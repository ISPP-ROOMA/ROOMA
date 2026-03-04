package com.example.demo.MemberApartment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.Apartment.ApartmentEntity;

public interface ApartmentMemberRepository extends JpaRepository<ApartmentMemberEntity, Integer> {
    List<ApartmentMemberEntity> findByApartmentId(Integer apartmentId);
    List<ApartmentMemberEntity> findByApartmentIdAndEndDateIsNull(Integer apartmentId);

    boolean existsByApartmentIdAndUserId(Integer apartmentId, Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.apartment.id = :apartmentId AND (m.endDate IS NULL OR m.endDate > CURRENT_DATE)")
    List<ApartmentMemberEntity> findActiveApartmentMembers(Integer apartmentId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.apartment.id = :apartmentId AND (m.endDate IS NULL OR m.endDate > CURRENT_DATE)")
    List<ApartmentMemberEntity> findCurrentTenantsByApartmentId(Integer apartmentId);
    
    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId ORDER BY COALESCE(m.endDate, CURRENT_DATE) DESC, m.joinDate DESC")
    Optional<ApartmentMemberEntity> findLastMembershipByUserId(Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND m.apartment.id = :apartmentId ORDER BY m.joinDate DESC")
    Optional<ApartmentMemberEntity> findByUserIdAndApartmentId(Integer userId, Integer apartmentId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND (m.endDate IS NULL OR m.endDate > CURRENT_DATE) ORDER BY m.joinDate DESC")
    List<ApartmentMemberEntity> findActiveMembershipsByUserId(Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND m.apartment.id = :apartmentId AND (m.endDate IS NULL OR m.endDate >= :cutoffDate) AND ((m.joinDate <= :joinDate AND (m.endDate IS NULL OR m.endDate >= :joinDate)) OR (m.joinDate <= :endDate AND (m.endDate IS NULL OR m.endDate >= :endDate)) OR (m.joinDate >= :joinDate AND (m.endDate IS NULL OR m.endDate <= :endDate)))")
    List<ApartmentMemberEntity> findOverlappingMemberships(Integer userId, Integer apartmentId, LocalDate joinDate, LocalDate endDate, LocalDate cutoffDate);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id <> :excludeUserId AND m.apartment.id = :apartmentId AND (m.endDate IS NULL OR m.endDate >= :cutoffDate) AND ((m.joinDate <= :joinDate AND (m.endDate IS NULL OR m.endDate >= :joinDate)) OR (m.joinDate <= :endDate AND (m.endDate IS NULL OR m.endDate >= :endDate)) OR (m.joinDate >= :joinDate AND (m.endDate IS NULL OR m.endDate <= :endDate)))")
    List<ApartmentMemberEntity> findOtherOverlappingMemberships(Integer excludeUserId, Integer apartmentId, LocalDate joinDate, LocalDate endDate, LocalDate cutoffDate);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId")
    List<ApartmentMemberEntity> findAllByUserId(Integer userId);

    @Query("SELECT am FROM ApartmentMemberEntity am WHERE am.user.id <> :userId AND am.apartment.id = :apartmentId AND (am.endDate IS NOT NULL AND am.endDate >= :cutoffDate AND am.endDate <= CURRENT_DATE)")
    List<ApartmentMemberEntity> findPastTenantMembershipsByUserIdAndApartmentId(Integer userId, Integer apartmentId, LocalDate cutoffDate);

    @Query("SELECT am FROM ApartmentMemberEntity am WHERE am.apartment.user.id = :userId AND am.apartment.id = :apartmentId AND (am.endDate IS NOT NULL AND am.endDate >= :cutoffDate AND am.endDate <= CURRENT_DATE)")
    List<ApartmentMemberEntity> findPastLandlordMembershipsByUserIdAndApartmentId(Integer userId, Integer apartmentId, LocalDate cutoffDate);

    @Query("SELECT DISTINCT am.apartment FROM ApartmentMemberEntity am WHERE am.user.id = :userId AND (am.endDate IS NULL OR (am.endDate >= :cutoffDate AND am.endDate <= CURRENT_DATE))")
    List<ApartmentEntity> findLastApartmentsByTenantIdAndApartmentId(Integer userId, LocalDate cutoffDate);

    @Query("SELECT DISTINCT am.apartment FROM ApartmentMemberEntity am WHERE am.apartment.user.id = :userId AND (am.endDate IS NOT NULL AND am.endDate >= :cutoffDate AND am.endDate <= CURRENT_DATE)")
    List<ApartmentEntity> findLastApartmentsByLandlordIdAndApartmentId(Integer userId, LocalDate cutoffDate);

    Optional<ApartmentMemberEntity> findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc(Integer userId);
    
    boolean existsByUserIdAndRole(Integer userId, MemberRole role);
}