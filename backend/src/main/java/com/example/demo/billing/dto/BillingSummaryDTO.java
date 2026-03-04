package com.example.demo.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingSummaryDTO(
        int pendingDebts,
        BigDecimal pendingAmount,
        LocalDate nextDueDate,
        String nextReference
) {}
