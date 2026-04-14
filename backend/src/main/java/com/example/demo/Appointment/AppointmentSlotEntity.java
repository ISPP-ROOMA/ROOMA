package com.example.demo.Appointment;

import java.time.LocalTime;

import com.example.demo.User.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointment_slots")
public class AppointmentSlotEntity {

    @Id
    @SequenceGenerator(name = "appointment_slots_seq", sequenceName = "appointment_slots_seq", initialValue = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appointment_slots_seq")
    private Integer id;

    @JoinColumn(name = "availability_block_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AvailabilityBlockEntity availabilityBlock;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @JoinColumn(name = "tenant_id", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    public AppointmentSlotEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AvailabilityBlockEntity getAvailabilityBlock() {
        return availabilityBlock;
    }

    public void setAvailabilityBlock(AvailabilityBlockEntity availabilityBlock) {
        this.availabilityBlock = availabilityBlock;
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

    public UserEntity getTenant() {
        return tenant;
    }

    public void setTenant(UserEntity tenant) {
        this.tenant = tenant;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
