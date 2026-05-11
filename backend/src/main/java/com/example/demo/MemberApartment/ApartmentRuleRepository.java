package com.example.demo.MemberApartment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApartmentRuleRepository extends JpaRepository<ApartmentRuleEntity, Integer> {

    Optional<ApartmentRuleEntity> findByApartmentId(Integer apartmentId);
}

