package com.example.demo.MemberApartment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.MemberApartment.DTOs.ReglaViviendaDTO;
import com.example.demo.MemberApartment.DTOs.UpdateReglaVivienda;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@Service
public class ReglaViviendaService {

    private final ReglaViviendaRepository reglaViviendaRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;

    public ReglaViviendaService(ReglaViviendaRepository reglaViviendaRepository,
                                ApartmentService apartmentService,
                                UserService userService) {
        this.reglaViviendaRepository = reglaViviendaRepository;
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public ReglaViviendaDTO getRules(Integer apartmentId) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        UserEntity currentUser = userService.findCurrentUserEntity();
        if (apartment.getUser() == null || !apartment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the landlord of this apartment can view its rules");
        }

        ReglaViviendaEntity entity = reglaViviendaRepository.findByViviendaId(apartmentId)
                .orElseGet(() -> {
                    ReglaViviendaEntity nueva = new ReglaViviendaEntity();
                    nueva.setVivienda(apartment);
                    nueva.setPermiteMascotas(false);
                    nueva.setPermiteFumadores(false);
                    nueva.setFiestasPermitidas(false);
                    return nueva;
                });

        return ReglaViviendaDTO.fromEntity(entity);
    }

    @Transactional
    public ReglaViviendaDTO updateRules(Integer apartmentId, UpdateReglaVivienda request) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        UserEntity currentUser = userService.findCurrentUserEntity();
        if (apartment.getUser() == null || !apartment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the landlord of this apartment can edit its rules");
        }

        ReglaViviendaEntity entity = reglaViviendaRepository.findByViviendaId(apartmentId)
                .orElseGet(() -> {
                    ReglaViviendaEntity nueva = new ReglaViviendaEntity();
                    nueva.setVivienda(apartment);
                    return nueva;
                });

        entity.setPermiteMascotas(request.permiteMascotas());
        entity.setPermiteFumadores(request.permiteFumadores());
        entity.setFiestasPermitidas(request.fiestasPermitidas());

        ReglaViviendaEntity saved = reglaViviendaRepository.save(entity);
        return ReglaViviendaDTO.fromEntity(saved);
    }
}

