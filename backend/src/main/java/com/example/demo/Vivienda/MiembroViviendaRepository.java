package com.example.demo.Vivienda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MiembroViviendaRepository extends JpaRepository<MiembroViviendaEntity, Integer> {
    List<MiembroViviendaEntity> findByViviendaId(Integer viviendaId);

    boolean existsByViviendaIdAndUsuarioId(Integer viviendaId, Integer usuarioId);
}




