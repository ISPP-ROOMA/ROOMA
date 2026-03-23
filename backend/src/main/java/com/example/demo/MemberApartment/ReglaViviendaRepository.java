package com.example.demo.MemberApartment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReglaViviendaRepository extends JpaRepository<ReglaViviendaEntity, Integer> {

    Optional<ReglaViviendaEntity> findByViviendaId(Integer viviendaId);
}

