package com.example.demo.Appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.ApartmentMatch.ApartmentMatchEntity;
import com.example.demo.ApartmentMatch.ApartmentMatchService;
import com.example.demo.ApartmentMatch.MatchStatus;
import com.example.demo.Appointment.DTOs.AppointmentSlotDTO;
import com.example.demo.Appointment.DTOs.AvailabilityBlockDTO;
import com.example.demo.Appointment.DTOs.CreateAvailabilityBlockDTO;
import com.example.demo.Chat.ChatService;
import com.example.demo.Exceptions.ConflictException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Notification.EventType;
import com.example.demo.Notification.NotificationService;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;

@Service
public class AppointmentService {

    private final AvailabilityBlockRepository availabilityBlockRepository;
    private final AppointmentSlotRepository appointmentSlotRepository;
    private final ApartmentService apartmentService;
    private final ApartmentMatchService matchService;
    private final ChatService chatService;
    private final UserService userService;
    private final NotificationService notificationService;

    public AppointmentService(AvailabilityBlockRepository availabilityBlockRepository,
            AppointmentSlotRepository appointmentSlotRepository,
            ApartmentService apartmentService,
            ApartmentMatchService matchService,
            ChatService chatService,
            UserService userService,
            NotificationService notificationService) {
        this.availabilityBlockRepository = availabilityBlockRepository;
        this.appointmentSlotRepository = appointmentSlotRepository;
        this.apartmentService = apartmentService;
        this.matchService = matchService;
        this.chatService = chatService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AvailabilityBlockDTO createBlock(Integer apartmentId, CreateAvailabilityBlockDTO request) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        if (!apartment.getUser().getId().equals(currentUser.getId())) {
            throw new ConflictException("Solo el dueño del apartamento puede organizar visitas.");
        }

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new ConflictException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        int duration = request.getSlotDurationMinutes() != null ? request.getSlotDurationMinutes() : 30;

        AvailabilityBlockEntity block = new AvailabilityBlockEntity();
        block.setApartment(apartment);
        block.setBlockDate(request.getBlockDate());
        block.setStartTime(request.getStartTime());
        block.setEndTime(request.getEndTime());
        block.setSlotDurationMinutes(duration);

        block = availabilityBlockRepository.save(block);

        List<AppointmentSlotEntity> slots = new ArrayList<>();
        LocalTime currentSlotTime = request.getStartTime();
        while (currentSlotTime.plusMinutes(duration).isBefore(request.getEndTime())
                || currentSlotTime.plusMinutes(duration).equals(request.getEndTime())) {
            AppointmentSlotEntity slot = new AppointmentSlotEntity();
            slot.setAvailabilityBlock(block);
            slot.setStartTime(currentSlotTime);
            slot.setEndTime(currentSlotTime.plusMinutes(duration));
            slot.setStatus(AppointmentStatus.AVAILABLE);
            slots.add(slot);
            currentSlotTime = currentSlotTime.plusMinutes(duration);
        }

        if (slots.isEmpty()) {
            throw new ConflictException("El rango de horas elegido es menor a la duracion de una cita.");
        }

        slots = appointmentSlotRepository.saveAll(slots);
        block.setSlots(slots);

        List<ApartmentMatchEntity> matches = matchService.findMatchesByApartmentIdAndMatchStatus(apartmentId,
                MatchStatus.MATCH);
        matches.addAll(matchService.findMatchesByApartmentIdAndMatchStatus(apartmentId, MatchStatus.SUCCESSFUL));
        matches.addAll(matchService.findMatchesByApartmentIdAndMatchStatus(apartmentId, MatchStatus.INVITED));

        String chatMessage = "He habilitado nuevas fechas para visitar el piso. Por favor, pulsa aquí para reservar tu cita y confirmar la visita.";
        String notificationDesc = "El arrendador " + currentUser.getName()
                + " ha abierto nuevas fechas para visitar el piso " + apartment.getTitle() + ".";
        String link = "/mis-solicitudes/recibidas";

        for (ApartmentMatchEntity match : matches) {
            try {
                notificationService.createNotification(EventType.APPOINTMENT, notificationDesc, link,
                        match.getCandidate());
                chatService.sendMessage(match.getId(), chatMessage, currentUser.getEmail());
            } catch (Exception e) {
                // Ignore error
            }
        }

        return AvailabilityBlockDTO.fromEntity(block);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityBlockDTO> getBlocksForApartment(Integer apartmentId) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (!apartment.getUser().getId().equals(currentUser.getId())) {
            throw new ConflictException("No tienes permiso.");
        }
        return availabilityBlockRepository
                .findByApartmentIdAndBlockDateGreaterThanEqualOrderByBlockDateAscStartTimeAsc(apartmentId,
                        LocalDate.now())
                .stream().map(AvailabilityBlockDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlotDTO> getAvailableSlotsForApartment(Integer apartmentId) {
        return appointmentSlotRepository
                .findByAvailabilityBlockApartmentIdAndStatus(apartmentId, AppointmentStatus.AVAILABLE)
                .stream()
                .filter(s -> s.getAvailabilityBlock().getBlockDate().isAfter(LocalDate.now()) ||
                        (s.getAvailabilityBlock().getBlockDate().equals(LocalDate.now())
                                && s.getStartTime().isAfter(LocalTime.now())))
                .map(AppointmentSlotDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentSlotDTO> getAvailableSlotsForMatch(Integer matchId) {
        ApartmentMatchEntity match = matchService.getMatchById(matchId);
        return getAvailableSlotsForApartment(match.getApartment().getId());
    }

    @Transactional
    public AppointmentSlotDTO bookSlot(Integer slotId) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        AppointmentSlotEntity slot = appointmentSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado"));

        if (slot.getStatus() != AppointmentStatus.AVAILABLE) {
            throw new ConflictException("Este horario ya no está disponible.");
        }

        List<AppointmentSlotEntity> existingBooks = appointmentSlotRepository
                .findByTenantIdOrderByAvailabilityBlockBlockDateAscStartTimeAsc(currentUser.getId());
        for (AppointmentSlotEntity b : existingBooks) {
            if (b.getStatus() == AppointmentStatus.BOOKED && b.getAvailabilityBlock().getApartment().getId()
                    .equals(slot.getAvailabilityBlock().getApartment().getId())) {
                throw new ConflictException("Ya tienes una cita reservada para este piso.");
            }
        }

        slot.setStatus(AppointmentStatus.BOOKED);
        slot.setTenant(currentUser);
        slot = appointmentSlotRepository.save(slot);

        ApartmentEntity apt = slot.getAvailabilityBlock().getApartment();
        String landlordMessage = "El inquilino " + currentUser.getName() + " ha reservado una visita para el piso "
                + apt.getTitle() +
                " el " + slot.getAvailabilityBlock().getBlockDate() + " a las " + slot.getStartTime() + ".";
        notificationService.createNotification(EventType.APPOINTMENT, landlordMessage, "/mis-solicitudes/recibidas",
                apt.getUser());

        try {
            ApartmentMatchEntity match = matchService.findApartmentMatchByCandidateAndApartment(currentUser.getId(),
                    apt.getId());
            chatService
                    .sendMessage(
                            match.getId(), "He reservado la visita para el "
                                    + slot.getAvailabilityBlock().getBlockDate() + " a las " + slot.getStartTime(),
                            currentUser.getEmail());
        } catch (Exception e) {
            // Ignore error
        }

        return AppointmentSlotDTO.fromEntity(slot);
    }

    @Transactional
    public AppointmentSlotDTO cancelSlot(Integer slotId) {
        UserEntity currentUser = userService.findCurrentUserEntity();
        AppointmentSlotEntity slot = appointmentSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot no encontrado"));

        ApartmentEntity apt = slot.getAvailabilityBlock().getApartment();
        boolean isTenant = slot.getTenant() != null && slot.getTenant().getId().equals(currentUser.getId());
        boolean isLandlord = apt.getUser().getId().equals(currentUser.getId());

        if (!isTenant && !isLandlord) {
            throw new ConflictException("Solo los involucrados pueden cancelar la reserva.");
        }

        if (slot.getStatus() != AppointmentStatus.BOOKED) {
            throw new ConflictException("No puedes cancelar un horario que no está reservado.");
        }

        UserEntity otherUser = isTenant ? apt.getUser() : slot.getTenant();
        UserEntity tenant = slot.getTenant();

        slot.setStatus(AppointmentStatus.AVAILABLE);
        slot.setTenant(null);
        slot = appointmentSlotRepository.save(slot);

        String message = "Tu cita del " + slot.getAvailabilityBlock().getBlockDate() + " a las "
                + slot.getStartTime().toString().substring(0, 5) + " ha sido cancelada.";
        notificationService.createNotification(EventType.APPOINTMENT, message, "/mis-solicitudes/recibidas", otherUser);

        try {
            Integer candidateId = isTenant ? currentUser.getId() : tenant.getId();
            ApartmentMatchEntity match = matchService.findApartmentMatchByCandidateAndApartment(candidateId,
                    apt.getId());
            chatService.sendMessage(match.getId(), message, currentUser.getEmail());
        } catch (Exception e) {
            // Ignore error
        }

        return AppointmentSlotDTO.fromEntity(slot);
    }
}
