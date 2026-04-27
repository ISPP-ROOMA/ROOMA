package com.example.demo.Apartment.DTOs;

import java.util.List;

import com.example.demo.ApartmentPhoto.dto.ApartmentPhotoDTO;
import com.example.demo.MemberApartment.DTOs.RoommateDTO;
import com.example.demo.billing.dto.BillingSummaryDTO;

public record ApartmentHomeDTO(
        ApartmentDTO apartment,
        List<RoommateDTO> roommates,
        List<ApartmentPhotoDTO> photos,
        BillingSummaryDTO billing,
        Integer openIncidences
) {}
