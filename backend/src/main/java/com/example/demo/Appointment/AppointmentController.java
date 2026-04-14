package com.example.demo.Appointment;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Appointment.DTOs.AppointmentSlotDTO;
import com.example.demo.Appointment.DTOs.AvailabilityBlockDTO;
import com.example.demo.Appointment.DTOs.CreateAvailabilityBlockDTO;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/blocks/apartment/{apartmentId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<AvailabilityBlockDTO> createBlock(
            @PathVariable Integer apartmentId,
            @RequestBody CreateAvailabilityBlockDTO request) {
        return ResponseEntity.ok(appointmentService.createBlock(apartmentId, request));
    }

    @GetMapping("/blocks/apartment/{apartmentId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<List<AvailabilityBlockDTO>> getBlocksForApartment(@PathVariable Integer apartmentId) {
        return ResponseEntity.ok(appointmentService.getBlocksForApartment(apartmentId));
    }

    @GetMapping("/apartment/{apartmentId}/available")
    public ResponseEntity<List<AppointmentSlotDTO>> getAvailableSlots(@PathVariable Integer apartmentId) {
        return ResponseEntity.ok(appointmentService.getAvailableSlotsForApartment(apartmentId));
    }

    @GetMapping("/match/{matchId}/available")
    public ResponseEntity<List<AppointmentSlotDTO>> getAvailableSlotsForMatch(@PathVariable Integer matchId) {
        return ResponseEntity.ok(appointmentService.getAvailableSlotsForMatch(matchId));
    }

    @PostMapping("/slots/{slotId}/book")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<AppointmentSlotDTO> bookSlot(@PathVariable Integer slotId) {
        return ResponseEntity.ok(appointmentService.bookSlot(slotId));
    }

    @PostMapping("/slots/{slotId}/cancel")
    public ResponseEntity<AppointmentSlotDTO> cancelSlot(@PathVariable Integer slotId) {
        return ResponseEntity.ok(appointmentService.cancelSlot(slotId));
    }
}
