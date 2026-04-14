package com.example.demo.Appointment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlotEntity, Integer> {
    List<AppointmentSlotEntity> findByAvailabilityBlockIdOrderByStartTimeAsc(Integer blockId);
    List<AppointmentSlotEntity> findByTenantIdOrderByAvailabilityBlockBlockDateAscStartTimeAsc(Integer tenantId);
    List<AppointmentSlotEntity> findByAvailabilityBlockApartmentIdAndStatus(Integer apartmentId, AppointmentStatus status);
}
