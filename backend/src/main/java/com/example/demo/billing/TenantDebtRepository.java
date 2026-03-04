package com.example.demo.billing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantDebtRepository extends JpaRepository<TenantDebtEntity, Integer> {

    List<TenantDebtEntity> findByUserIdAndStatus(Integer userId, DebtStatus status);
    List<TenantDebtEntity> findByUserId(Integer userId);
    List<TenantDebtEntity> findByBillId(Integer billId);
}
