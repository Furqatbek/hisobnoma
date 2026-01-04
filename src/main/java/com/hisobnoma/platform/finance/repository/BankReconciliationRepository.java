package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.BankReconciliation;
import com.hisobnoma.platform.finance.entity.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {

    @Query("SELECT br FROM BankReconciliation br WHERE br.tenantId = :tenantId ORDER BY br.statementDate DESC")
    List<BankReconciliation> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT br FROM BankReconciliation br WHERE br.tenantId = :tenantId")
    Page<BankReconciliation> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT br FROM BankReconciliation br WHERE br.id = :id AND br.tenantId = :tenantId")
    Optional<BankReconciliation> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT br FROM BankReconciliation br WHERE br.bankAccount.id = :bankAccountId AND br.tenantId = :tenantId ORDER BY br.statementDate DESC")
    List<BankReconciliation> findByBankAccountIdAndTenantId(@Param("bankAccountId") Long bankAccountId, @Param("tenantId") Long tenantId);

    @Query("SELECT br FROM BankReconciliation br WHERE br.bankAccount.id = :bankAccountId AND br.tenantId = :tenantId")
    Page<BankReconciliation> findByBankAccountIdAndTenantId(@Param("bankAccountId") Long bankAccountId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT br FROM BankReconciliation br WHERE br.bankAccount.id = :bankAccountId " +
           "AND br.status = :status AND br.tenantId = :tenantId ORDER BY br.statementDate DESC")
    List<BankReconciliation> findByBankAccountIdAndStatusAndTenantId(
            @Param("bankAccountId") Long bankAccountId,
            @Param("status") ReconciliationStatus status,
            @Param("tenantId") Long tenantId);

    @Query("SELECT br FROM BankReconciliation br WHERE br.bankAccount.id = :bankAccountId " +
           "AND br.status = 'COMPLETED' AND br.tenantId = :tenantId ORDER BY br.statementDate DESC")
    Optional<BankReconciliation> findLastCompletedByBankAccountId(
            @Param("bankAccountId") Long bankAccountId,
            @Param("tenantId") Long tenantId);

    @Query("SELECT br FROM BankReconciliation br WHERE br.bankAccount.id = :bankAccountId " +
           "AND br.status IN ('DRAFT', 'IN_PROGRESS') AND br.tenantId = :tenantId")
    Optional<BankReconciliation> findInProgressByBankAccountId(
            @Param("bankAccountId") Long bankAccountId,
            @Param("tenantId") Long tenantId);

    @Query("SELECT br FROM BankReconciliation br WHERE br.statementDate BETWEEN :startDate AND :endDate AND br.tenantId = :tenantId")
    List<BankReconciliation> findByDateRangeAndTenantId(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId);

    @Query("SELECT MAX(br.reconciliationNumber) FROM BankReconciliation br WHERE br.tenantId = :tenantId")
    String findMaxReconciliationNumberByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByReconciliationNumberAndTenantId(String reconciliationNumber, Long tenantId);

    @Query("SELECT COUNT(br) > 0 FROM BankReconciliation br WHERE br.bankAccount.id = :bankAccountId " +
           "AND br.status IN ('DRAFT', 'IN_PROGRESS') AND br.tenantId = :tenantId")
    boolean hasInProgressReconciliation(@Param("bankAccountId") Long bankAccountId, @Param("tenantId") Long tenantId);
}
