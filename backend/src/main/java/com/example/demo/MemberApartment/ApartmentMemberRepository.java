package com.example.demo.MemberApartment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.Apartment.ApartmentEntity;

public interface ApartmentMemberRepository extends JpaRepository<ApartmentMemberEntity, Integer> {
    List<ApartmentMemberEntity> findByApartmentId(Integer apartmentId);

    boolean existsByApartmentIdAndUserId(Integer apartmentId, Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.apartment.id = :apartmentId AND (m.leaveDate IS NULL OR m.leaveDate > CURRENT_DATE)")
    List<ApartmentMemberEntity> findActiveApartmentMembers(Integer apartmentId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.apartment.id = :apartmentId AND (m.leaveDate IS NULL OR m.leaveDate > CURRENT_DATE)")
    List<ApartmentMemberEntity> findCurrentTenantsByApartmentId(Integer apartmentId);
    
    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId ORDER BY COALESCE(m.leaveDate, CURRENT_DATE) DESC, m.joinDate DESC")
    Optional<ApartmentMemberEntity> findLastMembershipByUserId(Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND m.apartment.id = :apartmentId ORDER BY m.joinDate DESC")
    Optional<ApartmentMemberEntity> findByUserIdAndApartmentId(Integer userId, Integer apartmentId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND (m.leaveDate IS NULL OR m.leaveDate > CURRENT_DATE) ORDER BY m.joinDate DESC")
    List<ApartmentMemberEntity> findActiveMembershipsByUserId(Integer userId);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId AND m.apartment.id = :apartmentId AND (m.leaveDate IS NULL OR m.leaveDate >= :cutoffDate) AND ((m.joinDate <= :joinDate AND (m.leaveDate IS NULL OR m.leaveDate >= :joinDate)) OR (m.joinDate <= :leaveDate AND (m.leaveDate IS NULL OR m.leaveDate >= :leaveDate)) OR (m.joinDate >= :joinDate AND (m.leaveDate IS NULL OR m.leaveDate <= :leaveDate)))")
    List<ApartmentMemberEntity> findOverlappingMemberships(Integer userId, Integer apartmentId, LocalDate joinDate, LocalDate leaveDate, LocalDate cutoffDate);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id <> :excludeUserId AND m.apartment.id = :apartmentId AND (m.leaveDate IS NULL OR m.leaveDate >= :cutoffDate) AND ((m.joinDate <= :joinDate AND (m.leaveDate IS NULL OR m.leaveDate >= :joinDate)) OR (m.joinDate <= :leaveDate AND (m.leaveDate IS NULL OR m.leaveDate >= :leaveDate)) OR (m.joinDate >= :joinDate AND (m.leaveDate IS NULL OR m.leaveDate <= :leaveDate)))")
    List<ApartmentMemberEntity> findOtherOverlappingMemberships(Integer excludeUserId, Integer apartmentId, LocalDate joinDate, LocalDate leaveDate, LocalDate cutoffDate);

    @Query("SELECT m FROM ApartmentMemberEntity m WHERE m.user.id = :userId")
    List<ApartmentMemberEntity> findAllByUserId(Integer userId);

    @Query("SELECT am FROM ApartmentMemberEntity am WHERE am.user.id <> :userId AND am.apartment.id = :apartmentId AND (am.leaveDate IS NOT NULL AND am.leaveDate >= :cutoffDate AND am.leaveDate <= CURRENT_DATE)")
    List<ApartmentMemberEntity> findPastTenantMembershipsByUserIdAndApartmentId(Integer userId, Integer apartmentId, LocalDate cutoffDate);

    @Query("SELECT am FROM ApartmentMemberEntity am WHERE am.apartment.user.id = :userId AND am.apartment.id = :apartmentId AND (am.leaveDate IS NOT NULL AND am.leaveDate >= :cutoffDate AND am.leaveDate <= CURRENT_DATE)")
    List<ApartmentMemberEntity> findPastLandlordMembershipsByUserIdAndApartmentId(Integer userId, Integer apartmentId, LocalDate cutoffDate);

    @Query("SELECT DISTINCT am.apartment FROM ApartmentMemberEntity am WHERE am.user.id = :userId AND (am.leaveDate IS NULL OR (am.leaveDate >= :cutoffDate AND am.leaveDate <= CURRENT_DATE))")
    List<ApartmentEntity> findLastApartmentsByTenantIdAndApartmentId(Integer userId, LocalDate cutoffDate);

    @Query("SELECT DISTINCT am.apartment FROM ApartmentMemberEntity am WHERE am.apartment.user.id = :userId AND (am.leaveDate IS NOT NULL AND am.leaveDate >= :cutoffDate AND am.leaveDate <= CURRENT_DATE)")
    List<ApartmentEntity> findLastApartmentsByLandlordIdAndApartmentId(Integer userId, LocalDate cutoffDate);
}