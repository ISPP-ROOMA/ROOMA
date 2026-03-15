package com.example.demo.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import com.example.demo.billing.dto.BillingSummaryDTO;

@Service
public class BillingService {
    
    private final BillRepository billRepository;
    private final TenantDebtRepository tenantDebtRepository;
    private final UserService userService;
    private final ApartmentService apartmentService;
    private final ApartmentMemberService apartmentMemberService;

    public BillingService(BillRepository billRepository, TenantDebtRepository tenantDebtRepository,
            UserService userService, ApartmentService apartmentService,
            ApartmentMemberService apartmentMemberService) {
        this.billRepository = billRepository;
        this.tenantDebtRepository = tenantDebtRepository;
        this.userService = userService;
        this.apartmentService = apartmentService;
        this.apartmentMemberService = apartmentMemberService;
    }

    public List<BillEntity> getBillsForCurrentUser() {
        UserEntity currentUser = userService.findCurrentUserEntity();
        return billRepository.findByUserId(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public List<BillEntity> getBillsForApartment(Integer apartmentId) {
        if(apartmentId == null || apartmentService.findById(apartmentId) == null) {
            throw new ResourceNotFoundException("Apartment not found: " + apartmentId);
        }
        List<BillEntity> bills = billRepository.findByApartmentId(apartmentId);
        // force eager load of tenantDebts + user to avoid lazy init issues
        for (BillEntity bill : bills) {
            if (bill.getTenantDebts() != null) {
                bill.getTenantDebts().forEach(d -> {
                    if (d.getUser() != null) d.getUser().getEmail();
                });
            }
        }
        return bills;
    }

    public List<TenantDebtEntity> getDebtsForCurrentUser() {
        UserEntity currentUser = userService.findCurrentUserEntity();
        return tenantDebtRepository.findByUserId(currentUser.getId());
    }

    public List<TenantDebtEntity> getDebtsForCurrentUserByStatus(DebtStatus status) {
        if(DebtStatus.PAID != status && DebtStatus.PENDING != status) {
            throw new IllegalArgumentException("Invalid debt status: " + status);
        }
        UserEntity currentUser = userService.findCurrentUserEntity();
        return tenantDebtRepository.findByUserIdAndStatus(currentUser.getId(), status);
    }

    @Transactional
    public TenantDebtEntity payDebt(Integer debtId) {
        TenantDebtEntity debt = tenantDebtRepository.findById(debtId)
            .orElseThrow(() -> new ResourceNotFoundException("Debt not found: " + debtId));

        UserEntity currentUser = userService.findCurrentUserEntity();
        if (!debt.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Current user is not owner of this debt");
        }
        if (debt.getStatus() == DebtStatus.PAID) {
            throw new ForbiddenException("Debt is already paid");
        }
        debt.setStatus(DebtStatus.PAID);
        tenantDebtRepository.save(debt);

        Integer billId = debt.getBill().getId();
        List<TenantDebtEntity> debts = tenantDebtRepository.findByBillId(billId);
        boolean allPaid = debts.stream().allMatch(d -> d.getStatus() == DebtStatus.PAID);
        if (allPaid) {
            BillEntity bill = debt.getBill();
            bill.setStatus(BillStatus.PAID);
            billRepository.save(bill);
        }

        return debt;
    }

    @Transactional
    public BillEntity createBillAndSplit(BillEntity bill, Integer apartmentId) {
        if(bill == null) {
            throw new IllegalArgumentException("Bill cannot be null");
        }

        ApartmentEntity apartment = apartmentService.findById(apartmentId);
        if (apartment == null) {
            throw new ResourceNotFoundException("Apartment not found: " + apartmentId);
        }

        UserEntity landlord = userService.findCurrentUserEntity();

        if (!apartment.getUser().getId().equals(landlord.getId())) {
            throw new ForbiddenException("Only landlord can create bills for this apartment");
        }

        BillEntity billEntity = new BillEntity();
        billEntity.setReference(bill.getReference());
        billEntity.setTotalAmount(bill.getTotalAmount());
        billEntity.setStatus(BillStatus.PENDING);
        billEntity.setDuDate(bill.getDuDate());
        billEntity.setApartment(apartment);
        billEntity.setUser(landlord);

        List<ApartmentMemberEntity> members = apartmentMemberService.findCurrentMembers(apartmentId);
        if (members == null || members.isEmpty()) {
            throw new ResourceNotFoundException("No current members for apartment: " + apartmentId);
        }

        int count = members.size();
        BigDecimal share = bill.getTotalAmount().divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);

        List<TenantDebtEntity> debts = new ArrayList<>();
        for (ApartmentMemberEntity member : members) {
            TenantDebtEntity debt = new TenantDebtEntity();
            debt.setAmount(share);
            debt.setStatus(DebtStatus.PENDING);
            debt.setUser(member.getUser());
            debt.setBill(billEntity);
            debts.add(debt);
        }

        billEntity.setTenantDebts(debts);

        return billRepository.save(billEntity);
    }

    @Transactional(readOnly = true)
    public BillingSummaryDTO getBillingSummaryForUser(Integer userId) {
        if(userId == null || userService.findById(userId) == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        List<TenantDebtEntity> debts = tenantDebtRepository.findByUserId(userId);

        BigDecimal pendingAmount = debts.stream()
                .filter(debt -> debt.getStatus() == DebtStatus.PENDING)
                .map(TenantDebtEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int pendingCount = (int) debts.stream()
                .filter(debt -> debt.getStatus() == DebtStatus.PENDING)
                .count();

        LocalDate nextDueDate = debts.stream()
                .filter(debt -> debt.getStatus() == DebtStatus.PENDING)
                .map(TenantDebtEntity::getBill)
                .filter(Objects::nonNull)
                .map(BillEntity::getDuDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        String nextReference = debts.stream()
                .filter(debt -> debt.getStatus() == DebtStatus.PENDING)
                .sorted(Comparator.comparing(
                        d -> {
                            BillEntity bill = d.getBill();
                            return bill != null ? bill.getDuDate() : null;
                        },
                        Comparator.nullsLast(LocalDate::compareTo)))
                .map(TenantDebtEntity::getBill)
                .filter(Objects::nonNull)
                .map(BillEntity::getReference)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return new BillingSummaryDTO(pendingCount, pendingAmount, nextDueDate, nextReference);
    }




}
