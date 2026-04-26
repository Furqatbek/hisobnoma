package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.mapper.CustomerMapper;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CustomerDto customerDto;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .code("CUST-000001")
                .name("Test Customer")
                .email("test@example.com")
                .phone("+998901234567")
                .tenantId(TENANT_ID)
                .active(true)
                .creditHold(false)
                .currentBalance(BigDecimal.ZERO)
                .totalInvoiced(BigDecimal.ZERO)
                .totalReceived(BigDecimal.ZERO)
                .creditLimit(new BigDecimal("50000.00"))
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .contacts(new ArrayList<>())
                .build();

        customerDto = CustomerDto.builder()
                .id(1L)
                .code("CUST-000001")
                .name("Test Customer")
                .email("test@example.com")
                .phone("+998901234567")
                .active(true)
                .creditHold(false)
                .currentBalance(BigDecimal.ZERO)
                .creditLimit(new BigDecimal("50000.00"))
                .build();
    }

    @Test
    void shouldGetCustomersSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(customerMapper.toDto(customer)).thenReturn(customerDto);

        // When
        PageResponse<CustomerDto> result = customerService.getCustomers(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("CUST-000001", result.getContent().get(0).getCode());
    }

    @Test
    void shouldGetCustomerSuccessfully() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerMapper.toDto(customer)).thenReturn(customerDto);

        // When
        CustomerDto result = customerService.getCustomer(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Customer", result.getName());
    }

    @Test
    void shouldThrowNotFoundWhenCustomerDoesNotExist() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> customerService.getCustomer(999L));
    }

    @Test
    void shouldCreateCustomerSuccessfully() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUST-000001")
                .name("New Customer")
                .email("new@example.com")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.existsByCodeAndTenantId("CUST-000001", TENANT_ID)).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toDto(customer)).thenReturn(customerDto);

        // When
        CustomerDto result = customerService.createCustomer(request);

        // Then
        assertNotNull(result);
        assertEquals("CUST-000001", result.getCode());
        verify(customerRepository).save(any());
    }

    @Test
    void shouldAutoGenerateCodeWhenNotProvided() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("New Customer")
                .email("new@example.com")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findMaxCustomerCodeNumber(TENANT_ID)).thenReturn(5);
        when(customerRepository.existsByCodeAndTenantId("CUST-000006", TENANT_ID)).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toDto(customer)).thenReturn(customerDto);

        // When
        CustomerDto result = customerService.createCustomer(request);

        // Then
        assertNotNull(result);
        assertEquals("CUST-000006", request.getCode());
        verify(customerRepository).save(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenDuplicateCode() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUST-000001")
                .name("Duplicate Customer")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.existsByCodeAndTenantId("CUST-000001", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> customerService.createCustomer(request));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldUpdateCustomerSuccessfully() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUST-000001")
                .name("Updated Customer")
                .email("updated@example.com")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toDto(customer)).thenReturn(
                CustomerDto.builder()
                        .id(1L)
                        .code("CUST-000001")
                        .name("Updated Customer")
                        .email("updated@example.com")
                        .build());

        // When
        CustomerDto result = customerService.updateCustomer(1L, request);

        // Then
        assertNotNull(result);
        assertEquals("Updated Customer", result.getName());
        verify(customerMapper).updateEntity(eq(request), eq(customer));
        verify(customerRepository).save(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenUpdatingToDuplicateCode() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUST-000002")
                .name("Updated Customer")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByCodeAndTenantIdAndIdNot("CUST-000002", TENANT_ID, 1L)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> customerService.updateCustomer(1L, request));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void shouldDeleteCustomerSuccessfully() {
        // Given
        customer.setCurrentBalance(BigDecimal.ZERO);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        // When
        customerService.deleteCustomer(1L);

        // Then
        assertFalse(customer.isActive());
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldThrowBusinessExceptionWhenDeletingCustomerWithBalance() {
        // Given
        customer.setCurrentBalance(new BigDecimal("1000.00"));
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));

        // When/Then
        assertThrows(BusinessException.class, () -> customerService.deleteCustomer(1L));
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentCustomer() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> customerService.deleteCustomer(999L));
    }

    @Test
    void shouldSearchCustomersSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.searchByTenantId(TENANT_ID, "Test", pageable)).thenReturn(page);
        when(customerMapper.toDto(customer)).thenReturn(customerDto);

        // When
        PageResponse<CustomerDto> result = customerService.searchCustomers("Test", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(customerRepository).searchByTenantId(TENANT_ID, "Test", pageable);
    }

    @Test
    void shouldUpdateCustomerBalance() {
        // Given
        customer.setCurrentBalance(new BigDecimal("1000.00"));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        customerService.updateCustomerBalance(1L, new BigDecimal("500.00"));

        // Then
        assertEquals(new BigDecimal("1500.00"), customer.getCurrentBalance());
        verify(customerRepository).save(customer);
    }

    @Test
    void shouldUpdateCustomerBalanceWithNullCurrentBalance() {
        // Given
        customer.setCurrentBalance(null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        customerService.updateCustomerBalance(1L, new BigDecimal("500.00"));

        // Then
        assertEquals(new BigDecimal("500.00"), customer.getCurrentBalance());
    }

    @Test
    void shouldGetActiveCustomers() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(customer));
        when(customerMapper.toDtoList(List.of(customer))).thenReturn(List.of(customerDto));

        // When
        List<CustomerDto> result = customerService.getActiveCustomers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetCustomerByCode() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByCodeAndTenantId("CUST-000001", TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerMapper.toDto(customer)).thenReturn(customerDto);

        // When
        CustomerDto result = customerService.getCustomerByCode("CUST-000001");

        // Then
        assertNotNull(result);
        assertEquals("CUST-000001", result.getCode());
    }

    @Test
    void shouldActivateCustomer() {
        // Given
        customer.setActive(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerMapper.toDto(any(Customer.class))).thenReturn(
                CustomerDto.builder().id(1L).active(true).build());

        // When
        CustomerDto result = customerService.activateCustomer(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isActive());
        assertTrue(customer.isActive());
    }

    @Test
    void shouldSetCreditHold() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerMapper.toDto(any(Customer.class))).thenReturn(
                CustomerDto.builder().id(1L).creditHold(true).build());

        // When
        CustomerDto result = customerService.setCreditHold(1L, true);

        // Then
        assertNotNull(result);
        assertTrue(result.isCreditHold());
        assertTrue(customer.isCreditHold());
    }

    @Test
    void shouldUpdateCreditLimit() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerMapper.toDto(any(Customer.class))).thenReturn(
                CustomerDto.builder().id(1L).creditLimit(new BigDecimal("100000.00")).build());

        // When
        CustomerDto result = customerService.updateCreditLimit(1L, new BigDecimal("100000.00"));

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("100000.00"), result.getCreditLimit());
        assertEquals(new BigDecimal("100000.00"), customer.getCreditLimit());
    }

    @Test
    void shouldCanBeInvoicedReturnTrueWhenWithinLimit() {
        // Given
        customer.setCreditHold(false);
        customer.setCreditLimit(new BigDecimal("50000.00"));
        customer.setCurrentBalance(new BigDecimal("10000.00"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));

        // When
        boolean result = customerService.canBeInvoiced(1L, new BigDecimal("5000.00"));

        // Then
        assertTrue(result);
    }

    @Test
    void shouldCanBeInvoicedReturnFalseWhenOnCreditHold() {
        // Given
        customer.setCreditHold(true);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));

        // When
        boolean result = customerService.canBeInvoiced(1L, new BigDecimal("1000.00"));

        // Then
        assertFalse(result);
    }

    @Test
    void shouldCanBeInvoicedReturnFalseWhenExceedsLimit() {
        // Given
        customer.setCreditHold(false);
        customer.setCreditLimit(new BigDecimal("50000.00"));
        customer.setCurrentBalance(new BigDecimal("48000.00"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));

        // When
        boolean result = customerService.canBeInvoiced(1L, new BigDecimal("5000.00"));

        // Then
        assertFalse(result);
    }

    @Test
    void shouldCanBeInvoicedReturnTrueWhenNoCreditLimit() {
        // Given
        customer.setCreditHold(false);
        customer.setCreditLimit(null);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));

        // When
        boolean result = customerService.canBeInvoiced(1L, new BigDecimal("999999.00"));

        // Then
        assertTrue(result);
    }

    @Test
    void shouldGetCustomersOnCreditHold() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findCreditHoldByTenantId(TENANT_ID)).thenReturn(List.of(customer));
        when(customerMapper.toDtoList(List.of(customer))).thenReturn(List.of(customerDto));

        // When
        List<CustomerDto> result = customerService.getCustomersOnCreditHold();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetCustomersOverCreditLimit() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findOverCreditLimitByTenantId(TENANT_ID)).thenReturn(List.of(customer));
        when(customerMapper.toDtoList(List.of(customer))).thenReturn(List.of(customerDto));

        // When
        List<CustomerDto> result = customerService.getCustomersOverCreditLimit();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldGetNextCustomerCode() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findMaxCustomerCodeNumber(TENANT_ID)).thenReturn(10);

        // When
        String result = customerService.getNextCustomerCode();

        // Then
        assertEquals("CUST-000011", result);
    }
}
