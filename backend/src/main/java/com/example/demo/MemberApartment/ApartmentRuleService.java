package com.example.demo.MemberApartment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.MemberApartment.DTOs.ApartmentRuleDTO;
import com.example.demo.MemberApartment.DTOs.UpdateApartmentRule;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@Service
public class ApartmentRuleService {

    private final ApartmentRuleRepository apartmentRuleRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;

    public ApartmentRuleService(ApartmentRuleRepository apartmentRuleRepository,
                                ApartmentService apartmentService,
                                UserService userService) {
        this.apartmentRuleRepository = apartmentRuleRepository;
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
    public ApartmentRuleDTO updateRules(Integer apartmentId, UpdateApartmentRule request) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        UserEntity currentUser = userService.findCurrentUserEntity();
        if (apartment.getUser() == null || !apartment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the landlord of this apartment can edit its rules");
        }

        ApartmentRuleEntity entity = apartmentRuleRepository.findByApartmentId(apartmentId)
                .orElseGet(() -> {
                    ApartmentRuleEntity newRule = new ApartmentRuleEntity();
                    newRule.setApartment(apartment);
                    return newRule;
                });

        entity.setAllowsPets(request.allowsPets());
        entity.setAllowsSmokers(request.allowsSmokers());
        entity.setPartiesAllowed(request.partiesAllowed());

        ApartmentRuleEntity saved = apartmentRuleRepository.save(entity);
        return ApartmentRuleDTO.fromEntity(saved);
    }
}

