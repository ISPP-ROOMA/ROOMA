package com.example.demo.Billing;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.Exceptions.ResourceNotFoundException;
import com.example.demo.Jwt.JwtService;
import com.example.demo.User.UserEntity;
import com.example.demo.billing.BillEntity;
import com.example.demo.billing.BillStatus;
import com.example.demo.billing.BillingController;
import com.example.demo.billing.BillingService;
import com.example.demo.billing.DebtStatus;
import com.example.demo.billing.TenantDebtEntity;

@WebMvcTest(BillingController.class)
@Import(BillingControllerTests.SecurityTestConfig.class)
public class BillingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingService billingService;

    @MockitoBean
    private JwtService jwtService;

    @TestConfiguration
    @EnableMethodSecurity
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("myDebts should return 200 for tenant")
    public void myDebts_ReturnsOkForTenant() throws Exception {
        when(billingService.getDebtsForCurrentUser()).thenReturn(List.of());

        mockMvc.perform(get("/api/bills/me/debts"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("myDebts should deny access for unauthorized users")
    public void myDebts_UnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/bills/me/debts"))
               .andExpect(status().isForbidden());

        verify(billingService, never()).getDebtsForCurrentUser();
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("myDebtsByStatus should return 200 for tenant")
    public void myDebtsByStatus_ReturnsOkForTenant() throws Exception {
        when(billingService.getDebtsForCurrentUserByStatus(DebtStatus.PENDING)).thenReturn(List.of());

        mockMvc.perform(get("/api/bills/me/debts/status/pending"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("myDebtsByStatus should deny access for unauthorized users")
    public void myDebtsByStatus_UnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/bills/me/debts/status/pending"))
               .andExpect(status().isForbidden());

        verify(billingService, never()).getDebtsForCurrentUserByStatus(DebtStatus.PENDING);
        verify(billingService, never()).getDebtsForCurrentUserByStatus(DebtStatus.PAID);
        verify(billingService, never()).getDebtsForCurrentUserByStatus(DebtStatus.OVERDUE);
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("myDebtsByStatus should return exception if status is invalid")
    public void myDebtsByStatus_ReturnsExceptionIfStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/bills/me/debts/status/invalid"))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("payDebt should return 200 for tenant")
    public void payDebt_ReturnsOkForTenant() throws Exception {
        Integer debtId = 1;
        Integer userId = 2;
        Integer billId = 3;

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

        when(billingService.payDebt(debtId)).thenReturn(debt);

        mockMvc.perform(post("/api/bills/debts/{debtId}/pay", debtId))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("payDebt should deny access for unauthenticated users")
    public void payDebt_UnauthenticatedAccess() throws Exception {
        Integer debtId = 1;

        mockMvc.perform(post("/api/bills/debts/{debtId}/pay", debtId))
               .andExpect(status().isUnauthorized());

        verify(billingService, never()).payDebt(debtId);
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("payDebt should return exception if debt does not exist")
    public void payDebt_ReturnsExceptionIfDebtDoesNotExist() throws Exception {
        Integer debtId = 999;

        when(billingService.payDebt(debtId)).thenThrow(new ResourceNotFoundException("Debt not found"));

        mockMvc.perform(post("/api/bills/debts/{debtId}/pay", debtId))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("payDebt should return exception if user is not owner of the debt")
    public void payDebt_ReturnsExceptionIfUserIsNotOwner() throws Exception {
        Integer debtId = 1;

        when(billingService.payDebt(debtId)).thenThrow(new AccessDeniedException("You are not the owner of this debt"));

        mockMvc.perform(post("/api/bills/debts/{debtId}/pay", debtId))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("billsForApartment should return 200 for landlord")
    public void billsForApartment_ReturnsOkForLandlord() throws Exception {
        Integer apartmentId = 1;

        when(billingService.getBillsForApartment(apartmentId)).thenReturn(List.of());

        mockMvc.perform(get("/api/bills/apartments/{apartmentId}", apartmentId))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("billsForApartment should deny access for unauthorized users")
    public void billsForApartment_UnauthorizedAccess() throws Exception {
        Integer apartmentId = 1;

        mockMvc.perform(get("/api/bills/apartments/{apartmentId}", apartmentId))
               .andExpect(status().isForbidden());

        verify(billingService, never()).getBillsForApartment(apartmentId);
    }

    @Test
    @WithMockUser(roles= "LANDLORD")
    @DisplayName("billsForApartment should return exception if apartment does not exist")
    public void billsForApartment_ReturnsExceptionIfApartmentDoesNotExist() throws Exception {
        Integer apartmentId = 999;

        when(billingService.getBillsForApartment(apartmentId)).thenThrow(new ResourceNotFoundException("Apartment not found"));

        mockMvc.perform(get("/api/bills/apartments/{apartmentId}", apartmentId))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("billsForApartment should return exception if user is not landlord of the apartment")
    public void billsForApartment_ReturnsExceptionIfUserIsNotLandlord() throws Exception {
        Integer apartmentId = 1;

        when(billingService.getBillsForApartment(apartmentId)).thenThrow(new AccessDeniedException("You are not the landlord of this apartment"));

        mockMvc.perform(get("/api/bills/apartments/{apartmentId}", apartmentId))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("billsCreatedByMe should return 200 for tenant")
    public void billsCreatedByMe_ReturnsOkForTenant() throws Exception {
        when(billingService.getBillsForCurrentUser()).thenReturn(List.of());

        mockMvc.perform(get("/api/bills/me/bills"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("billsCreatedByMe should deny access for unauthenticated users")
    public void billsCreatedByMe_UnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/bills/me/bills"))
               .andExpect(status().isUnauthorized());

        verify(billingService, never()).getBillsForCurrentUser();
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("createBill should return 200 for landlord")
    public void createBill_ReturnsOkForLandlord() throws Exception {
        Integer apartmentId = 1;
        BillEntity bill = new BillEntity();
        bill.setId(1);

        when(billingService.createBillAndSplit(bill, apartmentId)).thenReturn(bill);

        mockMvc.perform(post("/api/bills/apartment/{apartmentId}", apartmentId)
               .contentType("application/json")
               .content("{\"id\": 1}"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("createBill should deny access for unauthorized users")
    public void createBill_UnauthorizedAccess() throws Exception {
        Integer apartmentId = 1;

        mockMvc.perform(post("/api/bills/apartment/{apartmentId}", apartmentId)
               .contentType("application/json")
               .content("{\"id\": 1}"))
               .andExpect(status().isForbidden());

        verify(billingService, never()).createBillAndSplit(new BillEntity(), apartmentId);
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("createBill should return exception if apartment does not exist")
    public void createBill_ReturnsExceptionIfApartmentDoesNotExist() throws Exception {
        Integer apartmentId = 999;
        BillEntity bill = new BillEntity();

        when(billingService.createBillAndSplit(bill, apartmentId)).thenThrow(new ResourceNotFoundException("Apartment not found"));

        mockMvc.perform(post("/api/bills/apartment/{apartmentId}", apartmentId)
               .contentType("application/json")
               .content("{}"))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("createBill should return exception if user is not landlord of the apartment")
    public void createBill_ReturnsExceptionIfUserIsNotLandlord() throws Exception {
        Integer apartmentId = 1;
        BillEntity bill = new BillEntity();

        when(billingService.createBillAndSplit(bill, apartmentId)).thenThrow(new AccessDeniedException("You are not the landlord of this apartment"));

        mockMvc.perform(post("/api/bills/apartment/{apartmentId}", apartmentId)
               .contentType("application/json")
               .content("{}"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    @DisplayName("createBill should return exception if apartmentId is not valid")
    public void createBill_ReturnsExceptionIfApartmentIdIsNotValid() throws Exception {
        Integer apartmentId = -1;
        BillEntity bill = new BillEntity();

        when(billingService.createBillAndSplit(bill, apartmentId)).thenThrow(new IllegalArgumentException("Invalid apartment ID"));

        mockMvc.perform(post("/api/bills/apartment/{apartmentId}", apartmentId)
               .contentType("application/json")
               .content("{}"))
               .andExpect(status().isBadRequest());
    }
}