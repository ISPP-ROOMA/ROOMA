package com.example.demo.ApartmentMatch;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentMatchRepository extends JpaRepository<ApartmentMatchEntity, Integer> {

    Optional<ApartmentMatchEntity> findByCandidateIdAndApartmentId(Integer candidateId, Integer apartmentId);

    List<ApartmentMatchEntity> findByCandidateId(Integer candidateId);

    List<ApartmentMatchEntity> findByApartmentId(Integer apartmentId);

    List<ApartmentMatchEntity> findByApartmentIdAndMatchStatus(Integer apartmentId, MatchStatus matchStatus);

    List<ApartmentMatchEntity> findByCandidateIdAndMatchStatus(Integer candidateId, MatchStatus matchStatus);
    
}
