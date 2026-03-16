package com.example.demo.Incident;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Integer> {
    
    @Query("SELECT i FROM IncidentEntity i WHERE i.apartment.id = :apartmentId")
    List<IncidentEntity> findByApartmentId(Integer apartmentId);

    

}
