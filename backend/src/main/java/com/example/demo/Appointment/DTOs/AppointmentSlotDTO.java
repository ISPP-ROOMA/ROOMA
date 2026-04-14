package com.example.demo.Appointment.DTOs;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.demo.Appointment.AppointmentSlotEntity;
import com.example.demo.Appointment.AppointmentStatus;
import com.example.demo.User.DTOs.UserDTO;

public class AppointmentSlotDTO {
    private Integer id;
    private Integer availabilityBlockId;
    private LocalDate blockDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private UserDTO tenant;
    private AppointmentStatus status;

    public static AppointmentSlotDTO fromEntity(AppointmentSlotEntity entity) {
        if (entity == null) return null;
        AppointmentSlotDTO dto = new AppointmentSlotDTO();
        dto.setId(entity.getId());
        dto.setAvailabilityBlockId(entity.getAvailabilityBlock() != null ? entity.getAvailabilityBlock().getId() : null);
        dto.setBlockDate(entity.getAvailabilityBlock() != null ? entity.getAvailabilityBlock().getBlockDate() : null);
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        if (entity.getTenant() != null) {
            dto.setTenant(UserDTO.fromUserEntity(entity.getTenant()));
        }
        dto.setStatus(entity.getStatus());
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAvailabilityBlockId() {
        return availabilityBlockId;
    }

    public void setAvailabilityBlockId(Integer availabilityBlockId) {
        this.availabilityBlockId = availabilityBlockId;
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

    public UserDTO getTenant() {
        return tenant;
    }

    public void setTenant(UserDTO tenant) {
        this.tenant = tenant;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
