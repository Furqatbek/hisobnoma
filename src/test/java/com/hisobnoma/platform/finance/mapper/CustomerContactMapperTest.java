package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateCustomerContactRequest;
import com.hisobnoma.platform.finance.dto.CustomerContactDto;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.entity.CustomerContact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CustomerContactMapperTest {

    @Autowired
    private CustomerContactMapper customerContactMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Customer customer = Customer.builder().id(10L).build();

        CustomerContact contact = CustomerContact.builder()
                .id(1L)
                .customer(customer)
                .name("John Doe")
                .title("Manager")
                .department("Sales")
                .email("john@example.com")
                .phone("+998901234567")
                .mobilePhone("+998901234568")
                .fax("+998711234567")
                .primary(true)
                .billingContact(true)
                .shippingContact(false)
                .notes("Primary contact")
                .active(true)
                .build();

        // When
        CustomerContactDto dto = customerContactMapper.toDto(contact);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getCustomerId());
        assertEquals("John Doe", dto.getName());
        assertEquals("Manager", dto.getTitle());
        assertEquals("Sales", dto.getDepartment());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("+998901234567", dto.getPhone());
        assertEquals("+998901234568", dto.getMobilePhone());
        assertEquals("+998711234567", dto.getFax());
        assertTrue(dto.isPrimary());
        assertTrue(dto.isBillingContact());
        assertFalse(dto.isShippingContact());
        assertEquals("Primary contact", dto.getNotes());
        assertTrue(dto.isActive());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(10L)
                .name("Jane Smith")
                .title("Director")
                .department("Finance")
                .email("jane@example.com")
                .phone("+998901111111")
                .mobilePhone("+998902222222")
                .fax("+998711111111")
                .primary(false)
                .billingContact(true)
                .shippingContact(true)
                .notes("Billing contact")
                .build();

        // When
        CustomerContact entity = customerContactMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("Jane Smith", entity.getName());
        assertEquals("Director", entity.getTitle());
        assertEquals("Finance", entity.getDepartment());
        assertEquals("jane@example.com", entity.getEmail());
        assertEquals("+998901111111", entity.getPhone());
        assertEquals("+998902222222", entity.getMobilePhone());
        assertEquals("+998711111111", entity.getFax());
        assertFalse(entity.isPrimary());
        assertTrue(entity.isBillingContact());
        assertTrue(entity.isShippingContact());
        assertEquals("Billing contact", entity.getNotes());
        assertTrue(entity.isActive()); // constant = "true"
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getCustomer());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        Customer customer = Customer.builder().id(10L).build();

        CustomerContact existing = CustomerContact.builder()
                .id(1L)
                .customer(customer)
                .name("Old Name")
                .title("Old Title")
                .active(true)
                .build();

        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(10L)
                .name("Updated Name")
                .title("Updated Title")
                .email("updated@example.com")
                .primary(true)
                .build();

        // When
        customerContactMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated Name", existing.getName());
        assertEquals("Updated Title", existing.getTitle());
        assertEquals("updated@example.com", existing.getEmail());
        assertTrue(existing.isPrimary());
        // ID and customer should remain unchanged
        assertEquals(1L, existing.getId());
        assertNotNull(existing.getCustomer());
    }

    @Test
    void toDtoList_mapsAllContacts() {
        // Given
        Customer customer = Customer.builder().id(10L).build();

        CustomerContact c1 = CustomerContact.builder()
                .id(1L).customer(customer).name("Contact 1").active(true).build();
        CustomerContact c2 = CustomerContact.builder()
                .id(2L).customer(customer).name("Contact 2").active(true).build();

        // When
        List<CustomerContactDto> dtos = customerContactMapper.toDtoList(List.of(c1, c2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Contact 1", dtos.get(0).getName());
        assertEquals("Contact 2", dtos.get(1).getName());
    }
}
