package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.BankTransaction;
import com.hisobnoma.platform.finance.entity.BankTransactionStatus;
import com.hisobnoma.platform.finance.entity.BankTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.tenantId = :tenantId ORDER BY bt.transactionDate DESC, bt.id DESC")
    List<BankTransaction> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.tenantId = :tenantId")
    Page<BankTransaction> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.id = :id AND bt.tenantId = :tenantId")
    Optional<BankTransaction> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId AND bt.tenantId = :tenantId ORDER BY bt.transactionDate DESC, bt.id DESC")
    List<BankTransaction> findByBankAccountIdAndTenantId(@Param("bankAccountId") Long bankAccountId, @Param("tenantId") Long tenantId);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId AND bt.tenantId = :tenantId")
    Page<BankTransaction> findByBankAccountIdAndTenantId(@Param("bankAccountId") Long bankAccountId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.transactionDate BETWEEN :startDate AND :endDate " +
           "AND bt.tenantId = :tenantId ORDER BY bt.transactionDate, bt.id")
    List<BankTransaction> findByBankAccountIdAndDateRangeAndTenantId(
            @Param("bankAccountId") Long bankAccountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.status = :status " +
           "AND bt.tenantId = :tenantId ORDER BY bt.transactionDate, bt.id")
    List<BankTransaction> findByBankAccountIdAndStatusAndTenantId(
            @Param("bankAccountId") Long bankAccountId,
            @Param("status") BankTransactionStatus status,
            @Param("tenantId") Long tenantId);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.status IN :statuses " +
           "AND bt.transactionDate <= :endDate " +
           "AND bt.tenantId = :tenantId ORDER BY bt.transactionDate, bt.id")
    List<BankTransaction> findUnreconciledByBankAccountIdAndTenantId(
            @Param("bankAccountId") Long bankAccountId,
            @Param("statuses") List<BankTransactionStatus> statuses,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.transactionType = :type AND bt.tenantId = :tenantId")
    List<BankTransaction> findByTransactionTypeAndTenantId(@Param("type") BankTransactionType type, @Param("tenantId") Long tenantId);

    @Query("SELECT bt FROM BankTransaction bt WHERE bt.referenceType = :refType AND bt.referenceId = :refId AND bt.tenantId = :tenantId")
    List<BankTransaction> findByReferenceAndTenantId(@Param("refType") String refType, @Param("refId") Long refId, @Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(bt.creditAmount - bt.debitAmount), 0) FROM BankTransaction bt " +
           "WHERE bt.bankAccount.id = :bankAccountId AND bt.status != 'VOIDED' AND bt.tenantId = :tenantId")
    BigDecimal calculateNetBalanceByBankAccountId(@Param("bankAccountId") Long bankAccountId, @Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(bt.creditAmount), 0) FROM BankTransaction bt " +
           "WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.transactionDate BETWEEN :startDate AND :endDate " +
           "AND bt.status = 'RECONCILED' " +
           "AND bt.tenantId = :tenantId")
    BigDecimal sumClearedDepositsByPeriod(
            @Param("bankAccountId") Long bankAccountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(bt.debitAmount), 0) FROM BankTransaction bt " +
           "WHERE bt.bankAccount.id = :bankAccountId " +
           "AND bt.transactionDate BETWEEN :startDate AND :endDate " +
           "AND bt.status = 'RECONCILED' " +
           "AND bt.tenantId = :tenantId")
    BigDecimal sumClearedWithdrawalsByPeriod(
            @Param("bankAccountId") Long bankAccountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("tenantId") Long tenantId);

    @Query("SELECT MAX(bt.transactionNumber) FROM BankTransaction bt WHERE bt.tenantId = :tenantId")
    String findMaxTransactionNumberByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByTransactionNumberAndTenantId(String transactionNumber, Long tenantId);
}
