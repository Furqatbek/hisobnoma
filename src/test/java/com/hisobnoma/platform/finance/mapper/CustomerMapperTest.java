package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(CustomerMapper.class);
    }

    @Test
    void toDto_mapsAllFields() {
        // Given
        Customer customer = Customer.builder()
                .code("CUST-000001")
                .name("Test Customer")
                .legalName("Test Customer LLC")
                .taxId("TAX-12345")
                .email("test@example.com")
                .phone("+998901234567")
                .altPhone("+998901234568")
                .mobilePhone("+998901234569")
                .address("123 Main St")
                .shippingAddress("456 Ship St")
                .city("Tashkent")
                .state("Tashkent")
                .country("Uzbekistan")
                .postalCode("100000")
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .creditLimit(new BigDecimal("50000.00"))
                .currentBalance(new BigDecimal("10000.00"))
                .totalInvoiced(new BigDecimal("100000.00"))
                .totalReceived(new BigDecimal("90000.00"))
                .defaultCurrency("UZS")
                .website("https://example.com")
                .notes("Customer notes")
                .active(true)
                .creditHold(false)
                .customerType("WHOLESALE")
                .contacts(new ArrayList<>())
                .build();
        customer.setId(1L);
        customer.setTenantId(1L);

        // When
        CustomerDto dto = mapper.toDto(customer);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("CUST-000001", dto.getCode());
        assertEquals("Test Customer", dto.getName());
        assertEquals("Test Customer LLC", dto.getLegalName());
        assertEquals("TAX-12345", dto.getTaxId());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("+998901234567", dto.getPhone());
        assertEquals("+998901234568", dto.getAltPhone());
        assertEquals("+998901234569", dto.getMobilePhone());
        assertEquals("123 Main St", dto.getAddress());
        assertEquals("456 Ship St", dto.getShippingAddress());
        assertEquals("Tashkent", dto.getCity());
        assertEquals("Tashkent", dto.getState());
        assertEquals("Uzbekistan", dto.getCountry());
        assertEquals("100000", dto.getPostalCode());
        assertEquals("Net 30", dto.getPaymentTerms());
        assertEquals(30, dto.getPaymentTermsDays());
        assertEquals(new BigDecimal("50000.00"), dto.getCreditLimit());
        assertEquals(new BigDecimal("10000.00"), dto.getCurrentBalance());
        assertEquals(new BigDecimal("100000.00"), dto.getTotalInvoiced());
        assertEquals(new BigDecimal("90000.00"), dto.getTotalReceived());
        assertEquals("UZS", dto.getDefaultCurrency());
        assertEquals("https://example.com", dto.getWebsite());
        assertEquals("Customer notes", dto.getNotes());
        assertTrue(dto.isActive());
        assertFalse(dto.isCreditHold());
        assertEquals("WHOLESALE", dto.getCustomerType());
    }

    @Test
    void toDto_mapsComputedFields() {
        // Given - customer over credit limit
        Customer customer = Customer.builder()
                .code("CUST-000002")
                .name("Over Limit Customer")
                .creditLimit(new BigDecimal("10000.00"))
                .currentBalance(new BigDecimal("15000.00"))
                .active(true)
                .creditHold(false)
                .contacts(new ArrayList<>())
                .build();
        customer.setId(2L);

        // When
        CustomerDto dto = mapper.toDto(customer);

        // Then
        assertNotNull(dto);
        assertTrue(dto.isOverCreditLimit());
        assertEquals(BigDecimal.ZERO, dto.getAvailableCredit());
    }

    @Test
    void toDto_mapsAvailableCreditCorrectly() {
        // Given - customer within credit limit
        Customer customer = Customer.builder()
                .code("CUST-000003")
                .name("Within Limit Customer")
                .creditLimit(new BigDecimal("50000.00"))
                .currentBalance(new BigDecimal("20000.00"))
                .active(true)
                .contacts(new ArrayList<>())
                .build();
        customer.setId(3L);

        // When
        CustomerDto dto = mapper.toDto(customer);

        // Then
        assertNotNull(dto);
        assertFalse(dto.isOverCreditLimit());
        assertEquals(new BigDecimal("30000.00"), dto.getAvailableCredit());
    }

    @Test
    void toDto_handlesNullCreditLimit() {
        // Given - customer with no credit limit
        Customer customer = Customer.builder()
                .code("CUST-000004")
                .name("No Limit Customer")
                .creditLimit(null)
                .currentBalance(new BigDecimal("99999.00"))
                .active(true)
                .contacts(new ArrayList<>())
                .build();
        customer.setId(4L);

        // When
        CustomerDto dto = mapper.toDto(customer);

        // Then
        assertNotNull(dto);
        assertFalse(dto.isOverCreditLimit());
        assertNull(dto.getAvailableCredit());
    }

    @Test
    void toEntity_mapsFromCreateRequest() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .code("CUST-000005")
                .name("New Customer")
                .legalName("New Customer LLC")
                .taxId("TAX-99999")
                .email("new@example.com")
                .phone("+998909999999")
                .address("789 New St")
                .city("Samarkand")
                .country("Uzbekistan")
                .paymentTerms("Net 15")
                .paymentTermsDays(15)
                .creditLimit(new BigDecimal("25000.00"))
                .defaultCurrency("UZS")
                .customerType("RETAIL")
                .active(true)
                .creditHold(false)
                .build();

        // When
        Customer entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("CUST-000005", entity.getCode());
        assertEquals("New Customer", entity.getName());
        assertEquals("New Customer LLC", entity.getLegalName());
        assertEquals("TAX-99999", entity.getTaxId());
        assertEquals("new@example.com", entity.getEmail());
        assertEquals("+998909999999", entity.getPhone());
        assertEquals("789 New St", entity.getAddress());
        assertEquals("Samarkand", entity.getCity());
        assertEquals("Uzbekistan", entity.getCountry());
        assertEquals("Net 15", entity.getPaymentTerms());
        assertEquals(15, entity.getPaymentTermsDays());
        assertEquals(new BigDecimal("25000.00"), entity.getCreditLimit());
        assertEquals("UZS", entity.getDefaultCurrency());
        assertEquals("RETAIL", entity.getCustomerType());
        assertTrue(entity.isActive());
        assertFalse(entity.isCreditHold());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
    }

    @Test
    void toEntity_handleMinimalRequest() {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name("Minimal Customer")
                .build();

        // When
        Customer entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("Minimal Customer", entity.getName());
        assertNull(entity.getCode());
        assertNull(entity.getEmail());
        assertNull(entity.getCreditLimit());
    }
}
