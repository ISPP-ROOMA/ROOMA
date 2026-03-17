package com.example.demo.Incident;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Integer> {

    List<IncidentEntity> findByApartmentIdOrderByCreatedAtDesc(Integer apartmentId);

    List<IncidentEntity> findByApartmentIdAndStatusInOrderByCreatedAtDesc(Integer apartmentId, List<IncidentStatus> statuses);

    List<IncidentEntity> findByStatusAndUpdatedAtBefore(IncidentStatus status, java.time.LocalDateTime threshold);

}
