package com.example.demo.Apartment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ApartmentRepository extends JpaRepository<ApartmentEntity, Integer> {

    public List<ApartmentEntity> findAllByUserId(Integer userId);
}
