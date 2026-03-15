package com.example.demo.Billing;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName; 
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentService;
import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.MemberApartment.ApartmentMemberEntity;
import com.example.demo.MemberApartment.ApartmentMemberService;
import com.example.demo.User.UserEntity;
import com.example.demo.User.UserService;
import com.example.demo.billing.BillEntity;
import com.example.demo.billing.BillRepository;
import com.example.demo.billing.BillStatus;
import com.example.demo.billing.BillingService;
import com.example.demo.billing.DebtStatus;
import com.example.demo.billing.TenantDebtEntity;
import com.example.demo.billing.TenantDebtRepository;
import com.example.demo.billing.dto.BillingSummaryDTO;

@ExtendWith(MockitoExtension.class)
public class BillingServiceTests {
    
    private BillingService billingService;

    @Mock
    private BillRepository billRepository;

    @Mock
    private TenantDebtRepository tenantDebtRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApartmentService apartmentService;

    @Mock
    private ApartmentMemberService apartmentMemberService;
    
    @BeforeEach
    public void setUp() {
        billingService = new BillingService(billRepository, tenantDebtRepository, userService, apartmentService, apartmentMemberService);
    }

    @Test
    @DisplayName("getBillsForCurrentUser should return bills for the current user")
    public void getBillsForCurrentUser_ReturnsBillsForCurrentUser() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        BillEntity bill1 = new BillEntity();
        BillEntity bill2 = new BillEntity();
        bill1.setUser(user);
        bill2.setUser(user);

        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(billRepository.findByUserId(userId)).thenReturn(List.of(bill1, bill2));

        List<BillEntity> bills = billingService.getBillsForCurrentUser();

        assertNotNull(bills);
        assertEquals(2, bills.size());

    }

    @Test
    @DisplayName("getBillsForCurrentUser should return empty list if no bills for current user")
    public void getBillsForCurrentUser_ReturnsEmptyListIfNoBillsForCurrentUser() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(billRepository.findByUserId(userId)).thenReturn(List.of());

        List<BillEntity> bills = billingService.getBillsForCurrentUser();

        assertNotNull(bills);
        assertEquals(0, bills.size());
    }

    @Test
    @DisplayName("getBillsForApartment should return bills for the given apartment")
    public void getBillsForApartment_ReturnsBillsForGivenApartment() {
        ApartmentEntity apartment = new ApartmentEntity();
        Integer apartmentId = 1;
        apartment.setId(apartmentId);

        BillEntity bill1 = new BillEntity();
        BillEntity bill2 = new BillEntity();
        bill1.setApartment(apartment);
        bill2.setApartment(apartment);

        when(billRepository.findByApartmentId(apartmentId)).thenReturn(List.of(bill1, bill2));

        List<BillEntity> bills = billingService.getBillsForApartment(apartmentId);

        assertNotNull(bills);
        assertEquals(2, bills.size());
    }

    @Test
    @DisplayName("getBillsForApartment should return empty list if no bills for given apartment")
    public void getBillsForApartment_ReturnsEmptyListIfNoBillsForGivenApartment() {
        Integer apartmentId = 1;

        when(billRepository.findByApartmentId(apartmentId)).thenReturn(List.of());

        List<BillEntity> bills = billingService.getBillsForApartment(apartmentId);

        assertNotNull(bills);
        assertEquals(0, bills.size());
    }

    @Test
    @DisplayName("getBillsForApartment should throw ResourceNotFoundException if apartment id is invalid")
    public void getBillsForApartment_ThrowsResourceNotFoundExceptionIfApartmentIdIsInvalid() {
        Integer apartmentId = 1;

        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class, 
            () -> billingService.getBillsForApartment(apartmentId));

        assertNotNull(exception);
    }

    @Test
    @DisplayName("getDebtsForCurrentUser should return debts for the current user")
    public void getDebtsForCurrentUser_ReturnsDebtsForCurrentUser() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        TenantDebtEntity debt1 = new TenantDebtEntity();
        TenantDebtEntity debt2 = new TenantDebtEntity();
        debt1.setUser(user);
        debt2.setUser(user);

        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(tenantDebtRepository.findByUserId(userId)).thenReturn(List.of(debt1, debt2));

        List<TenantDebtEntity> debts = billingService.getDebtsForCurrentUser();

        assertNotNull(debts);
        assertEquals(2, debts.size());
    }

    @Test
    @DisplayName("getDebtsForCurrentUser should return empty list if no debts for current user")
    public void getDebtsForCurrentUser_ReturnsEmptyListIfNoDebtsForCurrentUser() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(tenantDebtRepository.findByUserId(userId)).thenReturn(List.of());

        List<TenantDebtEntity> debts = billingService.getDebtsForCurrentUser();

        assertNotNull(debts);
        assertEquals(0, debts.size());
    }

    @Test
    @DisplayName("getDebtsForCurrentUserByStatus should return debts for the current user with given status")
    public void getDebtsForCurrentUserByStatus_ReturnsDebtsForCurrentUserWithGivenStatus() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        TenantDebtEntity debt1 = new TenantDebtEntity();
        TenantDebtEntity debt2 = new TenantDebtEntity();
        debt1.setUser(user);
        debt2.setUser(user);
        debt1.setStatus(DebtStatus.PAID);
        debt2.setStatus(DebtStatus.PAID);

        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(tenantDebtRepository.findByUserIdAndStatus(userId, DebtStatus.PAID)).thenReturn(List.of(debt1, debt2));

        List<TenantDebtEntity> debts = billingService.getDebtsForCurrentUserByStatus(DebtStatus.PAID);

        assertNotNull(debts);
        assertEquals(2, debts.size());
    }

    @Test
    @DisplayName("getDebtsForCurrentUserByStatus should return empty list if no debts for current user with given status")
    public void getDebtsForCurrentUserByStatus_ReturnsEmptyListIfNoDebtsForCurrentUserWithGivenStatus() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(tenantDebtRepository.findByUserIdAndStatus(userId, DebtStatus.PAID)).thenReturn(List.of());

        List<TenantDebtEntity> debts = billingService.getDebtsForCurrentUserByStatus(DebtStatus.PAID);

        assertNotNull(debts);
        assertEquals(0, debts.size());
    }

    @Test
    @DisplayName("getDebtsForCurrentUserByStatus should return empty list if user has no debts with given status")
    public void getDebtsForCurrentUserByStatus_ReturnsEmptyListIfUserHasNoDebtsWithGivenStatus() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        TenantDebtEntity debt1 = new TenantDebtEntity();
        debt1.setUser(user);
        debt1.setStatus(DebtStatus.PAID);
    
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(tenantDebtRepository.findByUserIdAndStatus(userId, DebtStatus.PENDING)).thenReturn(List.of());

        List<TenantDebtEntity> debts = billingService.getDebtsForCurrentUserByStatus(DebtStatus.PENDING);

        assertNotNull(debts);
        assertEquals(0, debts.size());
    }

    @Test
    @DisplayName("getDebtsForCurrentUserByStatus should throw IllegalArgumentException if status is invalid")
    public void getDebtsForCurrentUserByStatus_ThrowsIllegalArgumentExceptionIfStatusIsInvalid() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        when(userService.findCurrentUserEntity()).thenReturn(user);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> billingService.getDebtsForCurrentUserByStatus(null));

        assertNotNull(exception);
    }

    @Test
    @DisplayName("payDebt should mark the debt as paid and update bill status if all debts are paid")
    public void payDebt_MarksDebtAsPaidAndUpdatesBillStatusIfAllDebtsArePaid() {
        Integer debtId = 1;
        Integer userId = 1;
        Integer billId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);

        BillEntity bill = new BillEntity();
        bill.setId(billId);
        bill.setStatus(BillStatus.PENDING);

        TenantDebtEntity debt = new TenantDebtEntity();
        debt.setId(debtId);
        debt.setUser(user);
        debt.setBill(bill);
        debt.setStatus(DebtStatus.PENDING);

        when(tenantDebtRepository.save(any(TenantDebtEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantDebtRepository.findById(debtId)).thenReturn(java.util.Optional.of(debt));
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(tenantDebtRepository.findByBillId(billId)).thenReturn(List.of(debt));

        TenantDebtEntity updatedDebt = billingService.payDebt(debtId);

        assertNotNull(updatedDebt);
        assertEquals(DebtStatus.PAID, updatedDebt.getStatus());
        assertEquals(BillStatus.PAID, bill.getStatus());
    }

    @Test
    @DisplayName("payDebt should throw ResourceNotFoundException if debt id is invalid")
    public void payDebt_ThrowsResourceNotFoundExceptionIfDebtIdIsInvalid() {
        Integer debtId = 1;

        when(tenantDebtRepository.findById(debtId)).thenReturn(java.util.Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class, 
            () -> billingService.payDebt(debtId));

        assertNotNull(exception);
    }

    @Test
    @DisplayName("payDebt should throw ForbiddenException if current user is not owner of the debt")
    public void payDebt_ThrowsForbiddenExceptionIfCurrentUserIsNotOwnerOfTheDebt() {
        Integer debtId = 1;
        Integer userId = 1;
        Integer userId2 = 2;

        UserEntity user = new UserEntity();
        user.setId(userId);

        UserEntity user2 = new UserEntity();
        user2.setId(userId2);

        BillEntity bill = new BillEntity();
        bill.setId(1);
        bill.setStatus(BillStatus.PENDING);

        TenantDebtEntity debt = new TenantDebtEntity();
        debt.setId(debtId);
        debt.setUser(user2); // Set different user as owner of the debt
        debt.setBill(bill);
        debt.setStatus(DebtStatus.PENDING);

        when(tenantDebtRepository.findById(debtId)).thenReturn(java.util.Optional.of(debt));
        when(userService.findCurrentUserEntity()).thenReturn(user);

        ForbiddenException exception = assertThrows(
            ForbiddenException.class, 
            () -> billingService.payDebt(debtId));

        assertNotNull(exception);
    }

    @Test
    @DisplayName("payDebt should throw ForbiddenException if bill status is already paid")
    public void payDebt_ThrowsForbiddenExceptionIfBillStatusIsAlreadyPaid() {
        Integer debtId = 1;
        Integer userId = 1;
        Integer billId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);

        BillEntity bill = new BillEntity();
        bill.setId(billId);
        bill.setStatus(BillStatus.PAID); // Bill is already paid

        TenantDebtEntity debt = new TenantDebtEntity();
        debt.setId(debtId);
        debt.setUser(user);
        debt.setBill(bill);
        debt.setStatus(DebtStatus.PENDING);

        when(tenantDebtRepository.save(any(TenantDebtEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantDebtRepository.findById(debtId)).thenReturn(java.util.Optional.of(debt));
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(tenantDebtRepository.findByBillId(billId)).thenReturn(List.of(debt));

        ForbiddenException exception = assertThrows(
            ForbiddenException.class, 
            () -> billingService.payDebt(debtId));
        
        assertNotNull(exception);
    }

    @Test
    @DisplayName("payDebt should not update bill status if not all debts are paid")
    public void payDebt_DoesNotUpdateBillStatusIfNotAllDebtsArePaid() {
        Integer debtId1 = 1;
        Integer debtId2 = 2;
        Integer userId = 1;
        Integer billId = 1;

        UserEntity user = new UserEntity();
        user.setId(userId);

        BillEntity bill = new BillEntity();
        bill.setId(billId);
        bill.setStatus(BillStatus.PENDING);

        TenantDebtEntity debt1 = new TenantDebtEntity();
        debt1.setId(debtId1);
        debt1.setUser(user);
        debt1.setBill(bill);
        debt1.setStatus(DebtStatus.PENDING);

        TenantDebtEntity debt2 = new TenantDebtEntity();
        debt2.setId(debtId2);
        debt2.setUser(user);
        debt2.setBill(bill);
        debt2.setStatus(DebtStatus.PENDING);

        when(tenantDebtRepository.save(any(TenantDebtEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantDebtRepository.findById(debtId1)).thenReturn(java.util.Optional.of(debt1));
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(tenantDebtRepository.findByBillId(billId)).thenReturn(List.of(debt1, debt2));

        TenantDebtEntity updatedDebt = billingService.payDebt(debtId1);

        assertNotNull(updatedDebt);
        assertEquals(DebtStatus.PAID, updatedDebt.getStatus());
        assertEquals(BillStatus.PENDING, bill.getStatus());
    }

    @Test
    @DisplayName("createBillAndSplit should create a bill and split it into debts")
    public void createBillAndSplit_CreatesBillAndSplitsIntoDebts() {
        Integer apartmentId = 1;
        Integer userId1 = 1;
        Integer userId2 = 2;
        Integer userId3 = 3;

        UserEntity landlord = new UserEntity();
        landlord.setId(userId1);
        UserEntity tenant1 = new UserEntity();
        tenant1.setId(userId2);
        UserEntity tenant2 = new UserEntity();
        tenant2.setId(userId3);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        ApartmentMemberEntity member1 = new ApartmentMemberEntity();
        member1.setUser(tenant1);
        member1.setApartment(apartment);
        ApartmentMemberEntity member2 = new ApartmentMemberEntity();
        member2.setUser(tenant2);
        member2.setApartment(apartment);


        BillEntity bill = new BillEntity();
        bill.setReference("Test Bill");
        bill.setTotalAmount(new java.math.BigDecimal("100.00"));
        bill.setDuDate(java.time.LocalDate.now().plusDays(7));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMemberService.findCurrentMembers(apartmentId)).thenReturn(List.of(member1, member2));
        when(billRepository.save(any(BillEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        BillEntity createdBill = billingService.createBillAndSplit(bill, apartmentId);

        assertNotNull(createdBill);
        assertEquals(bill.getReference(), createdBill.getReference());
        assertEquals(bill.getTotalAmount(), createdBill.getTotalAmount());
        assertEquals(bill.getDuDate(), createdBill.getDuDate());
        assertEquals(apartment, createdBill.getApartment());
        assertEquals(landlord, createdBill.getUser());
        assertEquals(createdBill.getTenantDebts().size(), 2);
        assertEquals(tenant1, createdBill.getTenantDebts().get(0).getUser());
        assertEquals(new java.math.BigDecimal("50.00"), createdBill.getTenantDebts().get(0).getAmount());
    }

    @Test
    @DisplayName("createBillAndSplit should throw ResourceNotFoundException if apartment is not found")
    public void createBillAndSplit_ThrowsResourceNotFoundExceptionIfApartmentIsNotFound() {
        Integer apartmentId = 1;

        when(apartmentService.findById(apartmentId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> billingService.createBillAndSplit(new BillEntity(), apartmentId));
        assertEquals("Apartment not found: " + apartmentId, exception.getMessage());
    }

    @Test
    @DisplayName("createBillAndSplit should throw IllegalArgumentException if bill is null")
    public void createBillAndSplit_ThrowsIllegalArgumentExceptionIfBillIsNull() {
        Integer apartmentId = 1;

        when(apartmentService.findById(apartmentId)).thenReturn(new ApartmentEntity());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> billingService.createBillAndSplit(null, apartmentId));
        assertEquals("Bill cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("createBillAndSplit should throw ResourceNotFoundException if no current members for apartment")
    public void createBillAndSplit_ThrowsResourceNotFoundExceptionIfNoCurrentMembersForApartment() {
        Integer apartmentId = 1;

        when(apartmentService.findById(apartmentId)).thenReturn(new ApartmentEntity());
        when(apartmentMemberService.findCurrentMembers(apartmentId)).thenReturn(List.of());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> billingService.createBillAndSplit(new BillEntity(), apartmentId));
        assertEquals("No current members for apartment: " + apartmentId, exception.getMessage());
    }

    @Test
    @DisplayName("createBillAndSplit should throw ForbiddenException if current user is not landlord of the apartment")
    public void createBillAndSplit_ThrowsForbiddenExceptionIfCurrentUserIsNotLandlordOfTheApartment() {
        Integer apartmentId = 1;
        Integer userId = 1;
        Integer userId2 = 2;

        UserEntity landlord = new UserEntity();
        landlord.setId(userId);
        UserEntity user = new UserEntity();
        user.setId(userId2);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(apartmentMemberService.findCurrentMembers(apartmentId)).thenReturn(List.of());

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> billingService.createBillAndSplit(new BillEntity(), apartmentId));
        assertEquals("Only landlord can create bills for this apartment", exception.getMessage());
    }

    @Test
    @DisplayName("getBillingSummaryForUser should return billing summary for the user")
    public void getBillingSummaryForUser_ReturnsBillingSummaryForUser() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        TenantDebtEntity debt1 = new TenantDebtEntity();
        debt1.setUser(user);
        debt1.setAmount(new java.math.BigDecimal("50.00"));
        debt1.setStatus(DebtStatus.PAID);

        TenantDebtEntity debt2 = new TenantDebtEntity();
        debt2.setUser(user);
        debt2.setAmount(new java.math.BigDecimal("30.00"));
        debt2.setStatus(DebtStatus.PENDING);

        when(tenantDebtRepository.findByUserId(userId)).thenReturn(List.of(debt1, debt2));

        BillingSummaryDTO summary = billingService.getBillingSummaryForUser(userId);

        assertNotNull(summary);
        assertEquals(new java.math.BigDecimal("30.00"), summary.getPendingAmount());
        assertEquals(1, summary.getPendingDebts());
        assertEquals(null, summary.getNextDueDate());
        assertEquals(null, summary.getNextReference());
    }

    @Test
    @DisplayName("getBillingSummaryForUser should throw ResourceNotFoundException if user id is invalid")
    public void getBillingSummaryForUser_ThrowsResourceNotFoundExceptionIfUserIdIsInvalid() {
        Integer userId = 1;

        when(tenantDebtRepository.findByUserId(userId)).thenReturn(List.of());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> billingService.getBillingSummaryForUser(userId));
        assertEquals("User not found: " + userId, exception.getMessage());
    }

    @Test
    @DisplayName("getBillingSummaryForUser should return zero pending amount and debts if user has no debts")
    public void getBillingSummaryForUser_ReturnsZeroPendingAmountAndDebtsIfUserHasNoDebts() {
        Integer userId = 1;
        UserEntity user = new UserEntity();
        user.setId(userId);

        when(tenantDebtRepository.findByUserId(userId)).thenReturn(List.of());

        BillingSummaryDTO summary = billingService.getBillingSummaryForUser(userId);

        assertNotNull(summary);
        assertEquals(new java.math.BigDecimal("0.00"), summary.getPendingAmount());
        assertEquals(0, summary.getPendingDebts());
        assertEquals(null, summary.getNextDueDate());
        assertEquals(null, summary.getNextReference());
    }

    @Test
    @DisplayName("createBillAndSplit and payDebt integration test")
    public void createBillAndSplitAndPayDebt_IntegrationTest() {
        Integer apartmentId = 1;
        Integer userId1 = 1;
        Integer userId2 = 2;

        UserEntity landlord = new UserEntity();
        landlord.setId(userId1);
        UserEntity tenant = new UserEntity();
        tenant.setId(userId2);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);

        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setUser(tenant);
        member.setApartment(apartment);

        BillEntity bill = new BillEntity();
        bill.setReference("Integration Test Bill");
        bill.setTotalAmount(new java.math.BigDecimal("100.00"));
        bill.setDuDate(java.time.LocalDate.now().plusDays(7));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMemberService.findCurrentMembers(apartmentId)).thenReturn(List.of(member));
        when(billRepository.save(any(BillEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        BillEntity createdBill = billingService.createBillAndSplit(bill, apartmentId);

        assertNotNull(createdBill);
        assertEquals(1, createdBill.getTenantDebts().size());
        
        TenantDebtEntity debt = createdBill.getTenantDebts().get(0);
        
        when(tenantDebtRepository.findById(debt.getId())).thenReturn(java.util.Optional.of(debt));
        when(userService.findCurrentUserEntity()).thenReturn(tenant);
        when(tenantDebtRepository.findByBillId(createdBill.getId())).thenReturn(List.of(debt));

        TenantDebtEntity updatedDebt = billingService.payDebt(debt.getId());

        assertNotNull(updatedDebt);
        assertEquals(DebtStatus.PAID, updatedDebt.getStatus());
        assertEquals(BillStatus.PAID, createdBill.getStatus());
    }

    @Test
    @DisplayName("getBillsForApartment should increase when createBillAndSplit is called")
    public void getBillsForApartment_IncreasesWhenCreateBillAndSplitIsCalled() {
        Integer apartmentId = 1;
        Integer apartmentMemberId = 1;
        Integer userId = 1;
        Integer userId2 = 2;

        UserEntity user = new UserEntity();
        user.setId(userId);
        UserEntity landlord = new UserEntity();
        landlord.setId(userId2);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);
        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(apartmentMemberId);
        member.setApartment(apartment);
        member.setUser(user);

        BillEntity bill = new BillEntity();
        bill.setReference("Test Bill");
        bill.setTotalAmount(new java.math.BigDecimal("100.00"));
        bill.setDuDate(java.time.LocalDate.now().plusDays(7));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(billRepository.findByApartmentId(apartmentId)).thenReturn(List.of(bill));
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMemberService.findCurrentMembers(apartmentId)).thenReturn(List.of(member));

        billingService.createBillAndSplit(bill, apartmentId);

        List<BillEntity> bills = billingService.getBillsForApartment(apartmentId);

        assertNotNull(bills);
        assertEquals(1, bills.size());
    }

    @Test
    @DisplayName("getBillsForCurrentUser should increase when createBillAndSplit is called")
    public void getBillsForCurrentUser_IncreasesWhenCreateBillAndSplitIsCalled() {
        Integer apartmentId = 1;
        Integer apartmentMemberId = 1;
        Integer userId = 1;
        Integer userId2 = 2;

        UserEntity user = new UserEntity();
        user.setId(userId);
        UserEntity landlord = new UserEntity();
        landlord.setId(userId2);

        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setId(apartmentId);
        apartment.setUser(landlord);
        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setId(apartmentMemberId);
        member.setUser(user);
        member.setApartment(apartment);

        BillEntity bill = new BillEntity();
        bill.setReference("Test Bill");
        bill.setTotalAmount(new java.math.BigDecimal("100.00"));
        bill.setDuDate(java.time.LocalDate.now().plusDays(7));

        when(apartmentService.findById(apartmentId)).thenReturn(apartment);
        when(userService.findCurrentUserEntity()).thenReturn(landlord);
        when(apartmentMemberService.findCurrentMembers(apartmentId)).thenReturn(List.of(member));

        billingService.createBillAndSplit(bill, apartmentId);

        when(userService.findCurrentUserEntity()).thenReturn(user);
        when(billRepository.findByUserId(userId)).thenReturn(List.of(bill));
        List<BillEntity> bills = billingService.getBillsForCurrentUser();

        assertNotNull(bills);
        assertEquals(1, bills.size());
    }

}
