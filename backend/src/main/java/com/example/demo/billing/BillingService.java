package com.example.demo.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import org.springframework.transaction.annotation.Transactional;

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

    public List<BillEntity> getBillsForApartment(Integer apartmentId) {
        return billRepository.findByApartmentId(apartmentId);
    }

    public List<TenantDebtEntity> getDebtsForCurrentUser() {
        UserEntity currentUser = userService.findCurrentUserEntity();
        return tenantDebtRepository.findByUserId(currentUser.getId());
    }

    public List<TenantDebtEntity> getDebtsForCurrentUserByStatus(DebtStatus status) {
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

    public BillEntity createBillAndSplit(BillEntity bill, Integer apartmentId) {

        ApartmentEntity apartment = apartmentService.findById(apartmentId);

        UserEntity landlord = userService.findCurrentUserEntity();

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




}
