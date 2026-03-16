package com.example.demo.billing;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.example.demo.Exceptions.ForbiddenException;
import com.example.demo.Exceptions.ResourceNotFoundException;

@RestController
@RequestMapping("/api/bills")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/me/debts")
    @PreAuthorize("hasAnyRole('TENANT','ADMIN')")
    public ResponseEntity<List<TenantDebtEntity>> myDebts() {
        return ResponseEntity.ok(billingService.getDebtsForCurrentUser());
    }

    @GetMapping("/me/debts/status/{status}")
    @PreAuthorize("hasAnyRole('TENANT','ADMIN')")
    public ResponseEntity<List<TenantDebtEntity>> myDebtsByStatus(@PathVariable String status) throws IllegalArgumentException {
        DebtStatus ds = DebtStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(billingService.getDebtsForCurrentUserByStatus(ds));
    }

    @PostMapping("/debts/{debtId}/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantDebtEntity> payDebt(@PathVariable Integer debtId) throws ResourceNotFoundException, ForbiddenException {
        return ResponseEntity.ok(billingService.payDebt(debtId));
    }

    @GetMapping("/apartment/{apartmentId}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<List<BillEntity>> billsForApartment(@PathVariable Integer apartmentId) throws ResourceNotFoundException {
        return ResponseEntity.ok(billingService.getBillsForApartment(apartmentId));
    }

    @GetMapping("/me/bills")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BillEntity>> billsCreatedByMe() {
        return ResponseEntity.ok(billingService.getBillsForCurrentUser());
    }

    @PostMapping("/apartment/{apartmentId}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<BillEntity> createBill(@RequestBody BillEntity bill, @PathVariable Integer apartmentId) throws ResourceNotFoundException, ForbiddenException, IllegalArgumentException {
        return ResponseEntity.ok(billingService.createBillAndSplit(bill, apartmentId));
    }

}
