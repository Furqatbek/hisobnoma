package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateCustomerContactRequest;
import com.hisobnoma.platform.finance.dto.CustomerContactDto;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.entity.CustomerContact;
import com.hisobnoma.platform.finance.mapper.CustomerContactMapper;
import com.hisobnoma.platform.finance.repository.CustomerContactRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerContactServiceTest {

    @Mock
    private CustomerContactRepository customerContactRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerContactMapper customerContactMapper;

    @InjectMocks
    private CustomerContactService customerContactService;

    private Customer customer;
    private CustomerContact contact;
    private CustomerContactDto contactDto;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .code("CUST-000001")
                .name("Test Customer")
                .tenantId(1L)
                .active(true)
                .contacts(new ArrayList<>())
                .build();

        contact = CustomerContact.builder()
                .id(1L)
                .customer(customer)
                .name("John Doe")
                .title("Manager")
                .email("john@example.com")
                .phone("+998901234567")
                .primary(true)
                .billingContact(false)
                .shippingContact(false)
                .active(true)
                .build();

        contactDto = CustomerContactDto.builder()
                .id(1L)
                .customerId(1L)
                .name("John Doe")
                .title("Manager")
                .email("john@example.com")
                .phone("+998901234567")
                .primary(true)
                .billingContact(false)
                .shippingContact(false)
                .active(true)
                .build();
    }

    @Test
    void shouldGetContactsByCustomer() {
        // Given
        when(customerContactRepository.findByCustomerIdAndActiveTrue(1L)).thenReturn(List.of(contact));
        when(customerContactMapper.toDtoList(List.of(contact))).thenReturn(List.of(contactDto));

        // When
        List<CustomerContactDto> result = customerContactService.getContactsByCustomer(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }

    @Test
    void shouldGetContactSuccessfully() {
        // Given
        when(customerContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(customerContactMapper.toDto(contact)).thenReturn(contactDto);

        // When
        CustomerContactDto result = customerContactService.getContact(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getName());
    }

    @Test
    void shouldThrowNotFoundWhenContactDoesNotExist() {
        // Given
        when(customerContactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> customerContactService.getContact(999L));
    }

    @Test
    void shouldGetPrimaryContact() {
        // Given
        when(customerContactRepository.findByCustomerIdAndPrimaryTrue(1L)).thenReturn(Optional.of(contact));
        when(customerContactMapper.toDto(contact)).thenReturn(contactDto);

        // When
        CustomerContactDto result = customerContactService.getPrimaryContact(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isPrimary());
    }

    @Test
    void shouldThrowNotFoundWhenNoPrimaryContact() {
        // Given
        when(customerContactRepository.findByCustomerIdAndPrimaryTrue(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> customerContactService.getPrimaryContact(1L));
    }

    @Test
    void shouldGetBillingContact() {
        // Given
        CustomerContact billingContact = CustomerContact.builder()
                .id(2L)
                .customer(customer)
                .name("Billing Person")
                .billingContact(true)
                .active(true)
                .build();
        CustomerContactDto billingDto = CustomerContactDto.builder()
                .id(2L)
                .customerId(1L)
                .name("Billing Person")
                .billingContact(true)
                .build();

        when(customerContactRepository.findByCustomerIdAndBillingContactTrue(1L))
                .thenReturn(Optional.of(billingContact));
        when(customerContactMapper.toDto(billingContact)).thenReturn(billingDto);

        // When
        CustomerContactDto result = customerContactService.getBillingContact(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isBillingContact());
    }

    @Test
    void shouldReturnNullWhenNoBillingContact() {
        // Given
        when(customerContactRepository.findByCustomerIdAndBillingContactTrue(1L)).thenReturn(Optional.empty());

        // When
        CustomerContactDto result = customerContactService.getBillingContact(1L);

        // Then
        assertNull(result);
    }

    @Test
    void shouldGetShippingContact() {
        // Given
        CustomerContact shippingContact = CustomerContact.builder()
                .id(3L)
                .customer(customer)
                .name("Shipping Person")
                .shippingContact(true)
                .active(true)
                .build();
        CustomerContactDto shippingDto = CustomerContactDto.builder()
                .id(3L)
                .customerId(1L)
                .name("Shipping Person")
                .shippingContact(true)
                .build();

        when(customerContactRepository.findByCustomerIdAndShippingContactTrue(1L))
                .thenReturn(Optional.of(shippingContact));
        when(customerContactMapper.toDto(shippingContact)).thenReturn(shippingDto);

        // When
        CustomerContactDto result = customerContactService.getShippingContact(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isShippingContact());
    }

    @Test
    void shouldReturnNullWhenNoShippingContact() {
        // Given
        when(customerContactRepository.findByCustomerIdAndShippingContactTrue(1L)).thenReturn(Optional.empty());

        // When
        CustomerContactDto result = customerContactService.getShippingContact(1L);

        // Then
        assertNull(result);
    }

    @Test
    void shouldCreateContactSuccessfully() {
        // Given
        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(1L)
                .name("New Contact")
                .email("new@example.com")
                .phone("+998901111111")
                .primary(false)
                .build();

        CustomerContact newContact = CustomerContact.builder()
                .name("New Contact")
                .email("new@example.com")
                .phone("+998901111111")
                .primary(false)
                .active(true)
                .build();

        CustomerContactDto newDto = CustomerContactDto.builder()
                .id(2L)
                .customerId(1L)
                .name("New Contact")
                .email("new@example.com")
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerContactRepository.findByCustomerIdAndEmail(1L, "new@example.com"))
                .thenReturn(Optional.empty());
        when(customerContactMapper.toEntity(request)).thenReturn(newContact);
        when(customerContactRepository.save(any())).thenReturn(newContact);
        when(customerContactMapper.toDto(newContact)).thenReturn(newDto);

        // When
        CustomerContactDto result = customerContactService.createContact(request);

        // Then
        assertNotNull(result);
        assertEquals("New Contact", result.getName());
        verify(customerContactRepository).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenCustomerNotFoundOnCreate() {
        // Given
        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(999L)
                .name("Contact")
                .build();

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> customerContactService.createContact(request));
    }

    @Test
    void shouldThrowBusinessExceptionWhenDuplicateEmail() {
        // Given
        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(1L)
                .name("Duplicate Contact")
                .email("john@example.com")
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerContactRepository.findByCustomerIdAndEmail(1L, "john@example.com"))
                .thenReturn(Optional.of(contact));

        // When/Then
        assertThrows(BusinessException.class, () -> customerContactService.createContact(request));
    }

    @Test
    void shouldCreatePrimaryContactAndUnsetExisting() {
        // Given
        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(1L)
                .name("New Primary")
                .primary(true)
                .build();

        CustomerContact newPrimary = CustomerContact.builder()
                .name("New Primary")
                .primary(true)
                .active(true)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerContactRepository.findByCustomerIdAndPrimaryTrue(1L)).thenReturn(Optional.of(contact));
        when(customerContactMapper.toEntity(request)).thenReturn(newPrimary);
        when(customerContactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerContactMapper.toDto(any(CustomerContact.class))).thenReturn(
                CustomerContactDto.builder().id(2L).name("New Primary").primary(true).build());

        // When
        CustomerContactDto result = customerContactService.createContact(request);

        // Then
        assertNotNull(result);
        assertTrue(result.isPrimary());
        assertFalse(contact.isPrimary());
        verify(customerContactRepository, times(2)).save(any());
    }

    @Test
    void shouldUpdateContactSuccessfully() {
        // Given
        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(1L)
                .name("Updated Name")
                .email("updated@example.com")
                .primary(true)
                .build();

        when(customerContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(customerContactRepository.save(any())).thenReturn(contact);
        when(customerContactMapper.toDto(contact)).thenReturn(
                CustomerContactDto.builder().id(1L).name("Updated Name").build());

        // When
        CustomerContactDto result = customerContactService.updateContact(1L, request);

        // Then
        assertNotNull(result);
        verify(customerContactMapper).updateEntity(eq(request), eq(contact));
        verify(customerContactRepository).save(contact);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingNonExistentContact() {
        // Given
        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(1L)
                .name("Updated")
                .build();

        when(customerContactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> customerContactService.updateContact(999L, request));
    }

    @Test
    void shouldThrowBusinessExceptionWhenUpdatingToDuplicateEmail() {
        // Given
        CreateCustomerContactRequest request = CreateCustomerContactRequest.builder()
                .customerId(1L)
                .name("Updated")
                .email("other@example.com")
                .primary(false)
                .build();

        when(customerContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(customerContactRepository.existsByCustomerIdAndEmailAndIdNot(1L, "other@example.com", 1L))
                .thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> customerContactService.updateContact(1L, request));
    }

    @Test
    void shouldDeleteContactSuccessfully() {
        // Given
        when(customerContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(customerContactRepository.save(any())).thenReturn(contact);

        // When
        customerContactService.deleteContact(1L);

        // Then
        assertFalse(contact.isActive());
        verify(customerContactRepository).save(contact);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentContact() {
        // Given
        when(customerContactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> customerContactService.deleteContact(999L));
    }

    @Test
    void shouldSetPrimaryContactSuccessfully() {
        // Given
        CustomerContact existingPrimary = CustomerContact.builder()
                .id(2L)
                .customer(customer)
                .name("Old Primary")
                .primary(true)
                .active(true)
                .build();

        CustomerContact newPrimary = CustomerContact.builder()
                .id(3L)
                .customer(customer)
                .name("New Primary")
                .primary(false)
                .active(true)
                .build();

        when(customerContactRepository.findById(3L)).thenReturn(Optional.of(newPrimary));
        when(customerContactRepository.findByCustomerIdAndPrimaryTrue(1L)).thenReturn(Optional.of(existingPrimary));
        when(customerContactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerContactMapper.toDto(any(CustomerContact.class))).thenReturn(
                CustomerContactDto.builder().id(3L).name("New Primary").primary(true).build());

        // When
        CustomerContactDto result = customerContactService.setPrimaryContact(3L);

        // Then
        assertNotNull(result);
        assertTrue(result.isPrimary());
        assertFalse(existingPrimary.isPrimary());
        assertTrue(newPrimary.isPrimary());
    }

    @Test
    void shouldThrowNotFoundWhenSettingPrimaryForNonExistentContact() {
        // Given
        when(customerContactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> customerContactService.setPrimaryContact(999L));
    }
}
