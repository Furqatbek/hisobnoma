package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Customer> findByCodeAndTenantId(String code, Long tenantId);

    Page<Customer> findAllByTenantId(Long tenantId, Pageable pageable);

    List<Customer> findAllByTenantId(Long tenantId);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.active = true")
    List<Customer> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.active = true")
    Page<Customer> findAllActiveByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.creditHold = true")
    List<Customer> findCreditHoldByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.customerType = :customerType")
    List<Customer> findByTenantIdAndCustomerType(@Param("tenantId") Long tenantId, @Param("customerType") String customerType);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    boolean existsByCodeAndTenantIdAndIdNot(String code, Long tenantId, Long id);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.currentBalance > c.creditLimit AND c.creditLimit IS NOT NULL")
    List<Customer> findOverCreditLimitByTenantId(@Param("tenantId") Long tenantId);
}
