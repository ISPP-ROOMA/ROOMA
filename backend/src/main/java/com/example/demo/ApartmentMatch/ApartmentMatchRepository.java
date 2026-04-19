package com.example.demo.ApartmentMatch;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApartmentMatchRepository extends JpaRepository<ApartmentMatchEntity, Integer> {

    Optional<ApartmentMatchEntity> findByCandidateIdAndApartmentId(Integer candidateId, Integer apartmentId);

    List<ApartmentMatchEntity> findByCandidateId(Integer candidateId);

    List<ApartmentMatchEntity> findByApartmentId(Integer apartmentId);

    List<ApartmentMatchEntity> findByApartmentIdAndMatchStatus(Integer apartmentId, MatchStatus matchStatus);

    List<ApartmentMatchEntity> findByApartmentIdAndMatchStatusIn(Integer apartmentId, List<MatchStatus> matchStatuses);

    List<ApartmentMatchEntity> findByCandidateIdAndMatchStatus(Integer candidateId, MatchStatus matchStatus);
    
    @Query("SELECT am FROM ApartmentMatchEntity am WHERE am.apartment.user.id = :userId AND am.matchStatus = :matchStatus")
    List<ApartmentMatchEntity> findByUserIdAndMatchStatus(Integer userId, MatchStatus matchStatus);

    @Query("SELECT am FROM ApartmentMatchEntity am WHERE am.candidate.id = :userId AND am.matchStatus = :matchStatus")
    List<ApartmentMatchEntity> findTenantRequestByUserIdAndStatus (Integer userId, MatchStatus matchStatus);

}
