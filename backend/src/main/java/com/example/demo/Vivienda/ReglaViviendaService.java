package com.example.demo.Vivienda;

import com.example.demo.Exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReglaViviendaService {

    private final ReglaViviendaRepository reglaViviendaRepository;
    private final ViviendaRepository viviendaRepository;

    public ReglaViviendaService(ReglaViviendaRepository reglaViviendaRepository,
                               ViviendaRepository viviendaRepository) {
        this.reglaViviendaRepository = reglaViviendaRepository;
        this.viviendaRepository = viviendaRepository;
    }

    @Transactional(readOnly = true)
    public ReglaViviendaEntity getReglas(Integer viviendaId) {
        return reglaViviendaRepository.findByViviendaId(viviendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reglas no encontradas para la vivienda"));
    }

    @Transactional
    public ReglaViviendaEntity upsertReglas(Integer viviendaId, boolean permiteMascotas, boolean permiteFumadores, boolean fiestasPermitidas) {
        ViviendaEntity vivienda = viviendaRepository.findById(viviendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vivienda no encontrada"));

        ReglaViviendaEntity regla = reglaViviendaRepository.findByViviendaId(viviendaId)
                .orElseGet(ReglaViviendaEntity::new);

        regla.setVivienda(vivienda);
        regla.setPermiteMascotas(permiteMascotas);
        regla.setPermiteFumadores(permiteFumadores);
        regla.setFiestasPermitidas(fiestasPermitidas);

        return reglaViviendaRepository.save(regla);
    }
}




