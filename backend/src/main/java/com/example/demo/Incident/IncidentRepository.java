package com.example.demo.Incident;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Integer> {

    List<IncidentEntity> findByApartmentIdOrderByCreatedAtDesc(Integer apartmentId);

    List<IncidentEntity> findByApartmentIdAndStatusInOrderByCreatedAtDesc(Integer apartmentId, List<IncidentStatus> statuses);

    List<IncidentEntity> findByStatusAndUpdatedAtBefore(IncidentStatus status, java.time.LocalDateTime threshold);

    @Query("SELECT i FROM IncidentEntity i WHERE i.apartment.id = :apartmentId AND i.status NOT IN ('CLOSED', 'CLOSED_INACTIVITY') ORDER BY i.createdAt DESC")
    List<IncidentEntity> findIncidentsNotClosedByApartmentId(Integer apartmentId);

}
