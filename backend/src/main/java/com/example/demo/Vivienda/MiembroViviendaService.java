package com.example.demo.Vivienda;

import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MiembroViviendaService {

    private final MiembroViviendaRepository miembroViviendaRepository;
    private final ViviendaRepository viviendaRepository;
    private final UserRepository usuarioRepository;

    public MiembroViviendaService(MiembroViviendaRepository miembroViviendaRepository,
                                 ViviendaRepository viviendaRepository,
                                 UserRepository usuarioRepository) {
        this.miembroViviendaRepository = miembroViviendaRepository;
        this.viviendaRepository = viviendaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public MiembroViviendaEntity addMiembro(Integer viviendaId, Integer usuarioId, MiembroRol rol, LocalDate fechaIngreso) {
        ViviendaEntity vivienda = viviendaRepository.findById(viviendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vivienda no encontrada"));

        UserEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (miembroViviendaRepository.existsByViviendaIdAndUsuarioId(viviendaId, usuarioId)) {
            throw new BadRequestException("El usuario ya pertenece a esta vivienda");
        }

        MiembroViviendaEntity miembro = new MiembroViviendaEntity();
        miembro.setVivienda(vivienda);
        miembro.setUsuario(usuario);
        miembro.setRol(rol);
        miembro.setFechaIngreso(fechaIngreso != null ? fechaIngreso : LocalDate.now());

        return miembroViviendaRepository.save(miembro);
    }

    @Transactional(readOnly = true)
    public List<MiembroViviendaEntity> listMiembros(Integer viviendaId) {
        if (!viviendaRepository.existsById(viviendaId)) {
            throw new ResourceNotFoundException("Vivienda no encontrada");
        }
        return miembroViviendaRepository.findByViviendaId(viviendaId);
    }

    @Transactional
    public MiembroViviendaEntity updateRol(Integer viviendaId, Integer miembroId, MiembroRol rol) {
        MiembroViviendaEntity miembro = miembroViviendaRepository.findById(miembroId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));

        if (!miembro.getVivienda().getId().equals(viviendaId)) {
            throw new ResourceNotFoundException("Miembro no encontrado en la vivienda");
        }

        miembro.setRol(rol);
        return miembroViviendaRepository.save(miembro);
    }

    @Transactional
    public void removeMiembro(Integer viviendaId, Integer miembroId) {
        MiembroViviendaEntity miembro = miembroViviendaRepository.findById(miembroId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));

        if (!miembro.getVivienda().getId().equals(viviendaId)) {
            throw new ResourceNotFoundException("Miembro no encontrado en la vivienda");
        }

        miembroViviendaRepository.delete(miembro);
    }
}




