package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class CustomerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private Customer createCustomer(Long tenantId, String code, String name, String email,
                                     String phone, boolean active, boolean creditHold,
                                     String customerType, BigDecimal creditLimit,
                                     BigDecimal currentBalance) {
        Customer customer = Customer.builder()
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .email(email)
                .phone(phone)
                .active(active)
                .creditHold(creditHold)
                .customerType(customerType)
                .creditLimit(creditLimit)
                .currentBalance(currentBalance)
                .build();
        return entityManager.persistAndFlush(customer);
    }

    // ---- findByCodeAndTenantId ----

    @Test
    void findByCodeAndTenantId_exists_returnsCustomer() {
        createCustomer(TENANT_ID, "CUST-001", "Test Customer", "test@example.com",
                "1234567890", true, false, "RETAIL", null, BigDecimal.ZERO);

        Optional<Customer> result = customerRepository.findByCodeAndTenantId("CUST-001", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Test Customer", result.get().getName());
    }

    @Test
    void findByCodeAndTenantId_wrongTenant_returnsEmpty() {
        createCustomer(TENANT_ID, "CUST-001", "Test Customer", "test@example.com",
                "1234567890", true, false, "RETAIL", null, BigDecimal.ZERO);

        Optional<Customer> result = customerRepository.findByCodeAndTenantId("CUST-001", OTHER_TENANT_ID);

        assertFalse(result.isPresent());
    }

    // ---- findAllActiveByTenantId ----

    @Test
    void findAllActiveByTenantId_returnsActiveOnly() {
        createCustomer(TENANT_ID, "CUST-001", "Active Customer", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);
        createCustomer(TENANT_ID, "CUST-002", "Inactive Customer", null, null,
                false, false, "RETAIL", null, BigDecimal.ZERO);

        List<Customer> result = customerRepository.findAllActiveByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Active Customer", result.get(0).getName());
    }

    // ---- searchByTenantId ----

    @Test
    void searchByTenantId_byName_returnsMatching() {
        createCustomer(TENANT_ID, "CUST-001", "Acme Corporation", "acme@example.com",
                "1234567890", true, false, "RETAIL", null, BigDecimal.ZERO);
        createCustomer(TENANT_ID, "CUST-002", "Beta LLC", "beta@example.com",
                "0987654321", true, false, "WHOLESALE", null, BigDecimal.ZERO);

        Page<Customer> result = customerRepository.searchByTenantId(
                TENANT_ID, "acme", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("Acme Corporation", result.getContent().get(0).getName());
    }

    @Test
    void searchByTenantId_byCode_returnsMatching() {
        createCustomer(TENANT_ID, "CUST-001", "Test Customer", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);

        Page<Customer> result = customerRepository.searchByTenantId(
                TENANT_ID, "CUST-001", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchByTenantId_byEmail_returnsMatching() {
        createCustomer(TENANT_ID, "CUST-001", "Test Customer", "unique@example.com",
                null, true, false, "RETAIL", null, BigDecimal.ZERO);

        Page<Customer> result = customerRepository.searchByTenantId(
                TENANT_ID, "unique@", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    // ---- findCreditHoldByTenantId ----

    @Test
    void findCreditHoldByTenantId_returnsCreditHoldCustomers() {
        createCustomer(TENANT_ID, "CUST-001", "Normal Customer", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);
        createCustomer(TENANT_ID, "CUST-002", "Held Customer", null, null,
                true, true, "RETAIL", null, BigDecimal.ZERO);

        List<Customer> result = customerRepository.findCreditHoldByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Held Customer", result.get(0).getName());
    }

    // ---- findByTenantIdAndCustomerType ----

    @Test
    void findByTenantIdAndCustomerType_returnsMatchingType() {
        createCustomer(TENANT_ID, "CUST-001", "Retail Customer", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);
        createCustomer(TENANT_ID, "CUST-002", "Wholesale Customer", null, null,
                true, false, "WHOLESALE", null, BigDecimal.ZERO);

        List<Customer> result = customerRepository.findByTenantIdAndCustomerType(TENANT_ID, "WHOLESALE");

        assertEquals(1, result.size());
        assertEquals("Wholesale Customer", result.get(0).getName());
    }

    // ---- existsByCodeAndTenantId ----

    @Test
    void existsByCodeAndTenantId_exists_returnsTrue() {
        createCustomer(TENANT_ID, "CUST-001", "Test Customer", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);

        assertTrue(customerRepository.existsByCodeAndTenantId("CUST-001", TENANT_ID));
    }

    @Test
    void existsByCodeAndTenantId_notExists_returnsFalse() {
        assertFalse(customerRepository.existsByCodeAndTenantId("CUST-999", TENANT_ID));
    }

    // ---- existsByCodeAndTenantIdAndIdNot ----

    @Test
    void existsByCodeAndTenantIdAndIdNot_otherEntityWithSameCode_returnsTrue() {
        Customer c1 = createCustomer(TENANT_ID, "CUST-001", "Customer 1", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);
        Customer c2 = createCustomer(TENANT_ID, "CUST-002", "Customer 2", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);

        // Check if code "CUST-001" exists for tenant excluding c2's id -> true
        assertTrue(customerRepository.existsByCodeAndTenantIdAndIdNot("CUST-001", TENANT_ID, c2.getId()));
    }

    @Test
    void existsByCodeAndTenantIdAndIdNot_sameEntity_returnsFalse() {
        Customer c1 = createCustomer(TENANT_ID, "CUST-001", "Customer 1", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);

        // Check if code "CUST-001" exists for tenant excluding c1's own id -> false
        assertFalse(customerRepository.existsByCodeAndTenantIdAndIdNot("CUST-001", TENANT_ID, c1.getId()));
    }

    // ---- findOverCreditLimitByTenantId ----

    @Test
    void findOverCreditLimitByTenantId_returnsOverLimitCustomers() {
        createCustomer(TENANT_ID, "CUST-001", "Under Limit", null, null,
                true, false, "RETAIL", BigDecimal.valueOf(10000), BigDecimal.valueOf(5000));
        createCustomer(TENANT_ID, "CUST-002", "Over Limit", null, null,
                true, false, "RETAIL", BigDecimal.valueOf(10000), BigDecimal.valueOf(15000));
        createCustomer(TENANT_ID, "CUST-003", "No Limit", null, null,
                true, false, "RETAIL", null, BigDecimal.valueOf(50000));

        List<Customer> result = customerRepository.findOverCreditLimitByTenantId(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Over Limit", result.get(0).getName());
    }

    // ---- findMaxCustomerCodeNumber ----

    @Test
    void findMaxCustomerCodeNumber_returnsMax() {
        createCustomer(TENANT_ID, "CUST-010", "Customer 10", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);
        createCustomer(TENANT_ID, "CUST-055", "Customer 55", null, null,
                true, false, "RETAIL", null, BigDecimal.ZERO);

        Integer result = customerRepository.findMaxCustomerCodeNumber(TENANT_ID);

        assertEquals(55, result);
    }

    @Test
    void findMaxCustomerCodeNumber_noCustomers_returnsNull() {
        Integer result = customerRepository.findMaxCustomerCodeNumber(TENANT_ID);

        assertNull(result);
    }

    // ---- findMaxUpdatedAt ----

    @Test
    void findMaxUpdatedAt_noCustomers_returnsNull() {
        Instant result = customerRepository.findMaxUpdatedAt(TENANT_ID);

        assertNull(result);
    }

    // ---- searchByNameOrCodeOrPhone ----

    @Test
    void searchByNameOrCodeOrPhone_byPhone_returnsMatching() {
        createCustomer(TENANT_ID, "CUST-001", "Test Customer", "test@example.com",
                "9876543210", true, false, "RETAIL", null, BigDecimal.ZERO);

        Page<Customer> result = customerRepository.searchByNameOrCodeOrPhone(
                TENANT_ID, "98765", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }
}
