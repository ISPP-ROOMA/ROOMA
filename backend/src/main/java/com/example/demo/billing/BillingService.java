package com.example.demo.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.Notification.EventType;
import com.example.demo.Notification.NotificationService;
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
    private final NotificationService notificationService;

    public BillingService(BillRepository billRepository, TenantDebtRepository tenantDebtRepository,
            UserService userService, ApartmentService apartmentService,
            ApartmentMemberService apartmentMemberService, NotificationService notificationService) {
        this.billRepository = billRepository;
        this.tenantDebtRepository = tenantDebtRepository;
        this.userService = userService;
        this.apartmentService = apartmentService;
        this.apartmentMemberService = apartmentMemberService;
        this.notificationService = notificationService;
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
        BillEntity bill = debt.getBill();
        if (allPaid) {
            bill.setStatus(BillStatus.PAID);
            billRepository.save(bill);
        }

        notificationService.createNotification(
            EventType.BILL_PAID,
            "El inquilino " + currentUser.getName() + " ha pagado su parte de la factura.",
            "/apartments/" + bill.getApartment().getId() + "/bills",
            bill.getUser()
        );

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
        if (bill.getTotalAmount() == null || bill.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bill total amount must be greater than zero");
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

        Map<Integer, UserEntity> currentMembersByUserId = new HashMap<>();
        for (ApartmentMemberEntity member : members) {
            currentMembersByUserId.put(member.getUser().getId(), member.getUser());
        }

        List<TenantDebtEntity> debts = new ArrayList<>();
        List<TenantDebtEntity> incomingDebts = bill.getTenantDebts();
        if (incomingDebts != null && !incomingDebts.isEmpty()) {
            debts.addAll(buildCustomDebts(incomingDebts, billEntity, bill.getTotalAmount(), currentMembersByUserId));
        } else {
            debts.addAll(buildAutomaticDebts(members, billEntity, bill.getTotalAmount()));
        }

        Set<Integer> notifiedUsers = new HashSet<>();
        for (TenantDebtEntity debt : debts) {
            Integer userId = debt.getUser().getId();
            if (!userId.equals(landlord.getId()) && notifiedUsers.add(userId)) {
                notificationService.createNotification(
                    EventType.NEW_BILL,
                    "Tienes una nueva factura pendiente en el apartamento " + apartment.getTitle(),
                    "/invoices",
                    debt.getUser()
                );
            }
        }

        billEntity.setTenantDebts(debts);

        return billRepository.save(billEntity);
    }

    private List<TenantDebtEntity> buildAutomaticDebts(List<ApartmentMemberEntity> members, BillEntity billEntity, BigDecimal totalAmount) {
        int count = members.size();
        long totalCents = totalAmount
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        long baseShare = totalCents / count;
        long remainder = totalCents % count;

        List<TenantDebtEntity> debts = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            ApartmentMemberEntity member = members.get(i);
            long cents = baseShare + (i < remainder ? 1 : 0);

            TenantDebtEntity debt = new TenantDebtEntity();
            debt.setAmount(BigDecimal.valueOf(cents, 2));
            debt.setStatus(DebtStatus.PENDING);
            debt.setUser(member.getUser());
            debt.setBill(billEntity);
            debts.add(debt);
        }

        return debts;
    }

    private List<TenantDebtEntity> buildCustomDebts(
            List<TenantDebtEntity> incomingDebts,
            BillEntity billEntity,
            BigDecimal totalAmount,
            Map<Integer, UserEntity> currentMembersByUserId) {

        Map<Integer, BigDecimal> amountsByUser = new HashMap<>();
        for (TenantDebtEntity incomingDebt : incomingDebts) {
            if (incomingDebt.getUser() == null || incomingDebt.getUser().getId() == null) {
                throw new IllegalArgumentException("Each custom debt must include a valid user id");
            }
            if (incomingDebt.getAmount() == null || incomingDebt.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Each custom debt amount must be greater than zero");
            }

            Integer userId = incomingDebt.getUser().getId();
            if (!currentMembersByUserId.containsKey(userId)) {
                throw new IllegalArgumentException("Debt user is not a current apartment member: " + userId);
            }

            BigDecimal normalizedAmount = incomingDebt.getAmount().setScale(2, RoundingMode.HALF_UP);
            amountsByUser.merge(userId, normalizedAmount, BigDecimal::add);
        }

        BigDecimal customTotal = amountsByUser.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = totalAmount.setScale(2, RoundingMode.HALF_UP);

        if (customTotal.compareTo(expectedTotal) != 0) {
            throw new IllegalArgumentException("Custom split total must match bill total amount");
        }

        List<TenantDebtEntity> debts = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : amountsByUser.entrySet()) {
            TenantDebtEntity debt = new TenantDebtEntity();
            debt.setAmount(entry.getValue());
            debt.setStatus(DebtStatus.PENDING);
            debt.setUser(currentMembersByUserId.get(entry.getKey()));
            debt.setBill(billEntity);
            debts.add(debt);

        }

        return debts;
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
