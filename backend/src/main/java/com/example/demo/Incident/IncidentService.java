package com.example.demo.Incident;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Cloudinary.CloudinaryService;
import com.example.demo.Exceptions.BadRequestException;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Incident.DTOs.CreateIncidentRequest;
import com.example.demo.Incident.DTOs.IncidentDTO;
import com.example.demo.Incident.DTOs.IncidentStatusHistoryDTO;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@Service
public class IncidentService {

    private static final ZoneId MADRID_ZONE = ZoneId.of("Europe/Madrid");
    private static final Set<IncidentStatus> ACTIVE_STATUSES = EnumSet.of(
            IncidentStatus.OPEN,
            IncidentStatus.RECEIVED,
            IncidentStatus.IN_PROGRESS,
            IncidentStatus.TECHNICIAN_NOTIFIED,
            IncidentStatus.RESOLVED
    );
    private static final Set<IncidentStatus> CLOSED_STATUSES = EnumSet.of(
            IncidentStatus.CLOSED,
            IncidentStatus.CLOSED_INACTIVITY
    );

    private final IncidentRepository incidentRepository;
    private final IncidentStatusHistoryRepository incidentStatusHistoryRepository;
    private final UserService userService;
    private final ApartmentService apartmentService;
    private final ApartmentMemberService apartmentMemberService;
    private final CloudinaryService cloudinaryService;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentStatusHistoryRepository incidentStatusHistoryRepository,
                           UserService userService,
                           ApartmentService apartmentService,
                           ApartmentMemberService apartmentMemberService,
                           CloudinaryService cloudinaryService) {
        this.incidentRepository = incidentRepository;
        this.incidentStatusHistoryRepository = incidentStatusHistoryRepository;
        this.userService = userService;
        this.apartmentService = apartmentService;
        this.apartmentMemberService = apartmentMemberService;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public IncidentDTO findIncidentById(Integer apartmentId, Integer id) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        UserEntity currentUser = userService.findCurrentUserEntity();
        validateApartmentAccess(apartment, currentUser);

        IncidentEntity incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        validateIncidentBelongsToApartment(incident, apartmentId);
        return toDTO(incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentDTO> findIncidentsByApartmentId(Integer apartmentId, IncidentBucket bucket) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        UserEntity currentUser = userService.findCurrentUserEntity();
        validateApartmentAccess(apartment, currentUser);

        List<IncidentEntity> incidents;
        if (bucket == IncidentBucket.ACTIVE) {
            incidents = incidentRepository.findByApartmentIdAndStatusInOrderByCreatedAtDesc(
                    apartmentId,
                    ACTIVE_STATUSES.stream().toList()
            );
        } else if (bucket == IncidentBucket.CLOSED) {
            incidents = incidentRepository.findByApartmentIdAndStatusInOrderByCreatedAtDesc(
                    apartmentId,
                    CLOSED_STATUSES.stream().toList()
            );
        } else {
            incidents = incidentRepository.findByApartmentIdOrderByCreatedAtDesc(apartmentId);
        }

        return incidents.stream().map(this::toDTO).toList();
    }

    @Transactional
    public IncidentDTO createIncident(Integer apartmentId, CreateIncidentRequest request, List<MultipartFile> images) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (currentUser.getRole() != Role.TENANT) {
            throw new ForbiddenException("Only tenants can create incidents");
        }

        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        ApartmentMemberEntity apartmentMember = apartmentMemberService.findByUserIdAndApartmentId(currentUser.getId(), apartmentId);
        ensureMembershipIsActive(apartmentMember);

        IncidentEntity incident = new IncidentEntity();
        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setCategory(request.category());
        incident.setZone(request.zone());
        incident.setUrgency(request.urgency());
        incident.setPhotos(uploadIncidentImages(images));
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedAt(now());
        incident.setUpdatedAt(now());
        incident.setApartment(apartment);
        incident.setTenant(currentUser);
        incident.setLandlord(apartment.getUser());
        incident.setRejectionReason(null);

        IncidentEntity saved = incidentRepository.save(incident);
        appendStatusHistory(saved, IncidentStatus.OPEN, currentUser);
        return toDTO(saved);
    }

    @Transactional
    public IncidentDTO updateIncidentStatusByLandlord(Integer apartmentId, Integer id, IncidentStatus nextStatus) {
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (currentUser.getRole() != Role.LANDLORD) {
            throw new ForbiddenException("Only landlords can update incidents");
        }
        if (!apartment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the owner landlord can update incidents for this apartment");
        }

        IncidentEntity incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        validateIncidentBelongsToApartment(incident, apartmentId);

        if (nextStatus == IncidentStatus.CLOSED || nextStatus == IncidentStatus.CLOSED_INACTIVITY) {
            throw new BadRequestException("Landlord cannot directly close incidents");
        }
        if (!isValidLandlordTransition(incident.getStatus(), nextStatus)) {
            throw new BadRequestException("Invalid status transition");
        }

        applyStatusTransition(incident, nextStatus);
        incident.setRejectionReason(null);
        IncidentEntity saved = incidentRepository.save(incident);
        appendStatusHistory(saved, nextStatus, currentUser);
        return toDTO(saved);
    }

    @Transactional
    public IncidentDTO confirmSolutionByTenant(Integer apartmentId, Integer id) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (currentUser.getRole() != Role.TENANT) {
            throw new ForbiddenException("Only tenants can confirm the solution");
        }

        IncidentEntity incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        validateIncidentBelongsToApartment(incident, apartmentId);
        ensureTenantCanOperateIncident(incident, currentUser, apartmentId);

        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new BadRequestException("Only resolved incidents can be confirmed");
        }

        applyStatusTransition(incident, IncidentStatus.CLOSED);
        incident.setRejectionReason(null);
        IncidentEntity saved = incidentRepository.save(incident);
        appendStatusHistory(saved, IncidentStatus.CLOSED, currentUser);
        return toDTO(saved);
    }

    @Transactional
    public IncidentDTO rejectSolutionByTenant(Integer apartmentId, Integer id, String reason) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        if (currentUser.getRole() != Role.TENANT) {
            throw new ForbiddenException("Only tenants can reject the solution");
        }

        IncidentEntity incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        validateIncidentBelongsToApartment(incident, apartmentId);
        ensureTenantCanOperateIncident(incident, currentUser, apartmentId);

        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new BadRequestException("Only resolved incidents can be rejected");
        }

        incident.setRejectionReason(reason.trim());
        applyStatusTransition(incident, IncidentStatus.IN_PROGRESS);
        IncidentEntity saved = incidentRepository.save(incident);
        appendStatusHistory(saved, IncidentStatus.IN_PROGRESS, currentUser);
        return toDTO(saved);
    }

    @Transactional
    public void autoCloseResolvedIncidents() {
        LocalDateTime threshold = now().minusHours(72);
        List<IncidentEntity> staleResolved = incidentRepository.findByStatusAndUpdatedAtBefore(IncidentStatus.RESOLVED, threshold);

        for (IncidentEntity incident : staleResolved) {
            applyStatusTransition(incident, IncidentStatus.CLOSED_INACTIVITY);
            incident.setRejectionReason(null);
            IncidentEntity saved = incidentRepository.save(incident);
            appendStatusHistory(saved, IncidentStatus.CLOSED_INACTIVITY, saved.getTenant());
        }
    }

    private IncidentDTO toDTO(IncidentEntity incident) {
        List<IncidentStatusHistoryDTO> history = incidentStatusHistoryRepository
                .findByIncidentIdOrderByChangedAtAsc(incident.getId())
                .stream()
                .map(IncidentStatusHistoryDTO::fromEntity)
                .toList();

        return IncidentDTO.fromEntity(incident, history);
    }

    private void appendStatusHistory(IncidentEntity incident, IncidentStatus status, UserEntity actor) {
        IncidentStatusHistoryEntity history = new IncidentStatusHistoryEntity();
        history.setIncident(incident);
        history.setStatus(status);
        history.setChangedAt(now());
        history.setChangedByUserId(actor.getId());
        history.setChangedByEmail(actor.getEmail());
        incidentStatusHistoryRepository.save(history);
    }

    private void validateApartmentAccess(ApartmentEntity apartment, UserEntity currentUser) {
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.LANDLORD) {
            if (!apartment.getUser().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Landlord has no access to this apartment incidents");
            }
            return;
        }

        if (currentUser.getRole() == Role.TENANT) {
            ApartmentMemberEntity membership = apartmentMemberService.findByUserIdAndApartmentId(currentUser.getId(), apartment.getId());
            ensureMembershipIsActive(membership);
            return;
        }

        throw new ForbiddenException("User role cannot access incidents");
    }

    private void ensureTenantCanOperateIncident(IncidentEntity incident, UserEntity tenant, Integer apartmentId) {
        ApartmentMemberEntity membership = apartmentMemberService.findByUserIdAndApartmentId(tenant.getId(), apartmentId);
        ensureMembershipIsActive(membership);

        if (!incident.getTenant().getId().equals(tenant.getId())) {
            throw new ForbiddenException("Only the reporting tenant can confirm or reject this incident");
        }
    }

    private void ensureMembershipIsActive(ApartmentMemberEntity apartmentMember) {
        if (apartmentMember == null) {
            throw new ForbiddenException("User is not an active member of this apartment");
        }

        if (apartmentMember.getEndDate() != null && !apartmentMember.getEndDate().isAfter(LocalDate.now())) {
            throw new ForbiddenException("User is not an active member of this apartment");
        }
    }

    private void validateIncidentBelongsToApartment(IncidentEntity incident, Integer apartmentId) {
        if (!incident.getApartment().getId().equals(apartmentId)) {
            throw new ResourceNotFoundException("Incident not found for the provided apartment");
        }
    }

    private boolean isValidLandlordTransition(IncidentStatus current, IncidentStatus next) {
        if (current == next) {
            return false;
        }

        return switch (current) {
            case OPEN -> EnumSet.of(IncidentStatus.RECEIVED, IncidentStatus.IN_PROGRESS).contains(next);
            case RECEIVED -> EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.TECHNICIAN_NOTIFIED, IncidentStatus.RESOLVED).contains(next);
            case IN_PROGRESS -> EnumSet.of(IncidentStatus.TECHNICIAN_NOTIFIED, IncidentStatus.RESOLVED).contains(next);
            case TECHNICIAN_NOTIFIED -> EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED).contains(next);
            case RESOLVED, CLOSED, CLOSED_INACTIVITY -> false;
        };
    }

    private void applyStatusTransition(IncidentEntity incident, IncidentStatus nextStatus) {
        incident.setStatus(nextStatus);
        incident.setUpdatedAt(now());

        if (nextStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(now());
            incident.setClosedAt(null);
            return;
        }

        if (nextStatus == IncidentStatus.CLOSED || nextStatus == IncidentStatus.CLOSED_INACTIVITY) {
            incident.setClosedAt(now());
            if (incident.getResolvedAt() == null) {
                incident.setResolvedAt(now());
            }
            return;
        }

        incident.setClosedAt(null);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(MADRID_ZONE);
    }

    private List<String> uploadIncidentImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        if (images.size() > 5) {
            throw new BadRequestException("At most 5 images are allowed per incident");
        }

        return images.stream().map(file -> {
            try {
                var result = cloudinaryService.upload(file, "incidents");
                return (String) result.get("secure_url");
            } catch (IOException e) {
                throw new BadRequestException("Error uploading incident image to Cloudinary");
            }
        }).toList();
    }
}
