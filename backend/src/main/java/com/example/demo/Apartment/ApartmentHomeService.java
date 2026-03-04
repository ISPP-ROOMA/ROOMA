package com.example.demo.Apartment;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.DTOs.ApartmentDTO;
import com.example.demo.Apartment.DTOs.ApartmentHomeDTO;
import com.example.demo.ApartmentPhoto.ApartmentPhotoService;
import com.example.demo.ApartmentPhoto.dto.ApartmentPhotoDTO;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberRepository;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.MemberApartment.DTOs.RoommateDTO;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import com.example.demo.billing.BillingService;
import com.example.demo.billing.dto.BillingSummaryDTO;

@Service
public class ApartmentHomeService {

    private final UserService userService;
    private final ApartmentMemberRepository apartmentMemberRepository;
    private final ApartmentMemberService apartmentMemberService;
    private final ApartmentPhotoService apartmentPhotoService;
    private final BillingService billingService;

    public ApartmentHomeService(UserService userService,
                                ApartmentMemberRepository apartmentMemberRepository,
                                ApartmentMemberService apartmentMemberService,
                                ApartmentPhotoService apartmentPhotoService,
                                BillingService billingService) {
        this.userService = userService;
        this.apartmentMemberRepository = apartmentMemberRepository;
        this.apartmentMemberService = apartmentMemberService;
        this.apartmentPhotoService = apartmentPhotoService;
        this.billingService = billingService;
    }

    @Transactional(readOnly = true)
    public ApartmentHomeDTO getCurrentUserHome() {
        UserEntity currentUser = userService.findCurrentUserEntity();

        ApartmentMemberEntity membership = apartmentMemberRepository
                .findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not assigned to any apartment"));

        ApartmentEntity apartment = membership.getApartment();

        List<RoommateDTO> roommates = RoommateDTO.fromEntityList(
                apartmentMemberService.findCurrentMembers(apartment.getId()),
                currentUser.getId()
        );

        List<ApartmentPhotoDTO> photos = ApartmentPhotoDTO.fromEntityList(
                apartmentPhotoService.findPhotosByApartmentId(apartment.getId())
        );

        BillingSummaryDTO billingSummary = billingService.getBillingSummaryForUser(currentUser.getId());

        return new ApartmentHomeDTO(
                ApartmentDTO.fromApartmentEntity(apartment),
                roommates,
                photos,
                billingSummary
        );
    }
}
