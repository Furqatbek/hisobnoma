package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.BankAccount;
import com.hisobnoma.platform.finance.entity.BankAccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId ORDER BY ba.sortOrder, ba.accountCode")
    List<BankAccount> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId")
    Page<BankAccount> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.id = :id AND ba.tenantId = :tenantId")
    Optional<BankAccount> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.accountCode = :code AND ba.tenantId = :tenantId")
    Optional<BankAccount> findByAccountCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId AND ba.active = true ORDER BY ba.sortOrder, ba.accountCode")
    List<BankAccount> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId AND ba.accountType = :type ORDER BY ba.sortOrder, ba.accountCode")
    List<BankAccount> findByAccountTypeAndTenantId(@Param("type") BankAccountType type, @Param("tenantId") Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId AND ba.defaultAccount = true")
    Optional<BankAccount> findDefaultAccountByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId AND ba.allowPayments = true AND ba.active = true ORDER BY ba.sortOrder")
    List<BankAccount> findPaymentAccountsByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId AND ba.allowReceipts = true AND ba.active = true ORDER BY ba.sortOrder")
    List<BankAccount> findReceiptAccountsByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId AND ba.glAccount.id = :glAccountId")
    Optional<BankAccount> findByGlAccountIdAndTenantId(@Param("glAccountId") Long glAccountId, @Param("tenantId") Long tenantId);

    boolean existsByAccountCodeAndTenantId(String accountCode, Long tenantId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.tenantId = :tenantId " +
           "AND (LOWER(ba.accountCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(ba.accountName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(ba.bankName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<BankAccount> searchByTenantId(@Param("search") String search, @Param("tenantId") Long tenantId, Pageable pageable);

    // Mobile module method
    @Query("SELECT COALESCE(SUM(ba.currentBalance), 0) FROM BankAccount ba WHERE ba.tenantId = :tenantId AND ba.active = true")
    java.math.BigDecimal sumTotalBalance(@Param("tenantId") Long tenantId);
}
