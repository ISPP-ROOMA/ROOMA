package com.example.demo.Vivienda;

import com.example.demo.Exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ViviendaService {

    private final ViviendaRepository viviendaRepository;

    public ViviendaService(ViviendaRepository viviendaRepository) {
        this.viviendaRepository = viviendaRepository;
    }

    @Transactional
    public ViviendaEntity create(ViviendaEntity vivienda) {
        return viviendaRepository.save(vivienda);
    }

    @Transactional(readOnly = true)
    public ViviendaEntity findById(Integer id) {
        return viviendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vivienda no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<ViviendaEntity> findAll() {
        return viviendaRepository.findAll();
    }
}




