package com.example.demo.Incident;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentStatusHistoryRepository extends JpaRepository<IncidentStatusHistoryEntity, Integer> {
    List<IncidentStatusHistoryEntity> findByIncidentIdOrderByChangedAtAsc(Integer incidentId);
}
