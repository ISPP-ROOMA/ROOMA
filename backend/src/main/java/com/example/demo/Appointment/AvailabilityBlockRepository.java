package com.example.demo.Appointment;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvailabilityBlockRepository extends JpaRepository<AvailabilityBlockEntity, Integer> {
    List<AvailabilityBlockEntity> findByApartmentIdOrderByBlockDateAscStartTimeAsc(Integer apartmentId);
    List<AvailabilityBlockEntity> findByApartmentIdAndBlockDateGreaterThanEqualOrderByBlockDateAscStartTimeAsc(Integer apartmentId, LocalDate date);
}
