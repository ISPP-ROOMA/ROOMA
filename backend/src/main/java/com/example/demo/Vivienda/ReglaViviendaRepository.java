package com.example.demo.Vivienda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReglaViviendaRepository extends JpaRepository<ReglaViviendaEntity, Integer> {
    Optional<ReglaViviendaEntity> findByViviendaId(Integer viviendaId);
}




