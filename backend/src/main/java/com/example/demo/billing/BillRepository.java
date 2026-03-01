package com.example.demo.billing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillRepository extends JpaRepository<BillEntity, Integer> {

    List<BillEntity> findByApartmentId(Integer apartmentId);
    List<BillEntity> findByUserId(Integer userId);
}
