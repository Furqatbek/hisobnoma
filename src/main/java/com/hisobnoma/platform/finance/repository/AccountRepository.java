package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId ORDER BY a.code")
    List<Account> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId")
    Page<Account> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.parentAccount IS NULL ORDER BY a.code")
    List<Account> findRootAccountsByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.parentAccount.id = :parentId ORDER BY a.code")
    List<Account> findChildAccountsByParentId(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.code = :code")
    Optional<Account> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.tenantId = :tenantId")
    Optional<Account> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.accountType = :accountType ORDER BY a.code")
    List<Account> findByAccountTypeAndTenantId(@Param("tenantId") Long tenantId, @Param("accountType") AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.active = true ORDER BY a.code")
    List<Account> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.active = true AND a.allowsDirectPosting = true ORDER BY a.code")
    List<Account> findAllPostableByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Account> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.childAccounts WHERE a.id = :id")
    Optional<Account> findByIdWithChildren(@Param("id") Long id);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.controlAccount = true ORDER BY a.code")
    List<Account> findControlAccountsByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.systemAccount = true ORDER BY a.code")
    List<Account> findSystemAccountsByTenantId(@Param("tenantId") Long tenantId);
}
