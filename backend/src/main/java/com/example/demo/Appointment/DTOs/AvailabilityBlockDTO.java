package com.example.demo.Appointment.DTOs;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.Appointment.AvailabilityBlockEntity;

public class AvailabilityBlockDTO {
    private Integer id;
    private Integer apartmentId;
    private LocalDate blockDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private List<AppointmentSlotDTO> slots;

    public static AvailabilityBlockDTO fromEntity(AvailabilityBlockEntity entity) {
        if (entity == null) return null;
        AvailabilityBlockDTO dto = new AvailabilityBlockDTO();
        dto.setId(entity.getId());
        dto.setApartmentId(entity.getApartment() != null ? entity.getApartment().getId() : null);
        dto.setBlockDate(entity.getBlockDate());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setSlotDurationMinutes(entity.getSlotDurationMinutes());
        if (entity.getSlots() != null) {
            dto.setSlots(entity.getSlots().stream().map(AppointmentSlotDTO::fromEntity).collect(Collectors.toList()));
        }
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(Integer apartmentId) {
        this.apartmentId = apartmentId;
    }

    public LocalDate getBlockDate() {
        return blockDate;
    }

    public void setBlockDate(LocalDate blockDate) {
        this.blockDate = blockDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(Integer slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public List<AppointmentSlotDTO> getSlots() {
        return slots;
    }

    public void setSlots(List<AppointmentSlotDTO> slots) {
        this.slots = slots;
    }
}
