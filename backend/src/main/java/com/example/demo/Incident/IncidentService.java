package com.example.demo.Incident;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;


@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserService userService;
    private final ApartmentService apartmentService;
    private final ApartmentMemberService apartmentMemberService;

    public IncidentService(IncidentRepository incidentRepository, UserService userService, ApartmentService apartmentService, ApartmentMemberService apartmentMemberService) {
        this.incidentRepository = incidentRepository;
        this.userService = userService;
        this.apartmentService = apartmentService;
        this.apartmentMemberService = apartmentMemberService;
    }

    @Transactional(readOnly = true)
    public IncidentEntity findIncidentById(Integer id) {
        return incidentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    }

    @Transactional(readOnly = true)
    public List<IncidentEntity> findIncidentsByApartmentId(Integer apartmentId) {
        return incidentRepository.findByApartmentId(apartmentId);
    }

    @Transactional
    public IncidentEntity createIncident(Integer apartmentId, IncidentEntity newIncident) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (currentUser.getRole() != Role.TENANT) {
            throw new IllegalArgumentException("Only tenants can create incidents");
        }
        ApartmentMemberEntity apartmentMember = apartmentMemberService.findByUserIdAndApartmentId(apartmentId, currentUser.getId());
        if (apartmentMember == null) {
            throw new IllegalArgumentException("User is not a member of the apartment");
        }
        IncidentEntity incident = new IncidentEntity();
        incident.setTitle(newIncident.getTitle());
        incident.setDescription(newIncident.getDescription());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedAt(LocalDateTime.now(ZoneId.of("Europe/Madrid")));
        incident.setApartment(apartmentService.findById(apartmentId));
        incident.setTenant(currentUser);
        incident.setLandlord(apartmentMember.getApartment().getUser());
        return incidentRepository.save(incident);
    }

    @Transactional
    public IncidentEntity updateIncident(Integer id, IncidentEntity updatedIncident) {
        IncidentEntity incident = findIncidentById(id);
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (currentUser.getRole() != Role.LANDLORD) {
            throw new IllegalArgumentException("Only landlords can update incidents");
        }
        if(incident.getLandlord() == null || !incident.getLandlord().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("User is not the landlord of the apartment");
        }

        if (updatedIncident.getStatus() != null && updatedIncident.getStatus() != incident.getStatus()) {
            incident.setStatus(updatedIncident.getStatus());
            if (updatedIncident.getStatus() == IncidentStatus.RESOLVED) {
                incident.setResolvedAt(LocalDateTime.now(ZoneId.of("Europe/Madrid")));
            }
        }
        return incidentRepository.save(incident);
    }


}
