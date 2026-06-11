package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateVendorContactRequest;
import com.hisobnoma.platform.inventory.dto.VendorContactDto;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.entity.VendorContact;
import com.hisobnoma.platform.inventory.mapper.VendorContactMapper;
import com.hisobnoma.platform.inventory.repository.VendorContactRepository;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorContactServiceTest {

    @Mock
    private VendorContactRepository vendorContactRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private VendorContactMapper vendorContactMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private VendorContactService vendorContactService;

    private Vendor vendor;
    private VendorContact contact;
    private VendorContactDto contactDto;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(1L);
        vendor = Vendor.builder()
                .id(1L)
                .code("VND-001")
                .name("Acme Supplies")
                .active(true)
                .build();

        contact = VendorContact.builder()
                .id(1L)
                .vendor(vendor)
                .name("John Doe")
                .email("john@acme.com")
                .phone("+998901234567")
                .primary(true)
                .billingContact(false)
                .orderingContact(false)
                .active(true)
                .build();

        contactDto = VendorContactDto.builder()
                .id(1L)
                .vendorId(1L)
                .name("John Doe")
                .email("john@acme.com")
                .phone("+998901234567")
                .primary(true)
                .active(true)
                .build();
    }

    @Test
    void getContactsByVendor_returnsList() {
        // Given
        when(vendorContactRepository.findByVendorIdAndActiveTrue(1L)).thenReturn(List.of(contact));
        when(vendorContactMapper.toDtoList(List.of(contact))).thenReturn(List.of(contactDto));

        // When
        List<VendorContactDto> result = vendorContactService.getContactsByVendor(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }

    @Test
    void getContact_found_returnsDto() {
        // Given
        when(vendorContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(vendorContactMapper.toDto(contact)).thenReturn(contactDto);

        // When
        VendorContactDto result = vendorContactService.getContact(1L);

        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
    }

    @Test
    void getContact_notFound_throwsNotFoundException() {
        // Given
        when(vendorContactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorContactService.getContact(999L));
    }

    @Test
    void getPrimaryContact_found_returnsDto() {
        // Given
        when(vendorContactRepository.findByVendorIdAndPrimaryTrue(1L)).thenReturn(Optional.of(contact));
        when(vendorContactMapper.toDto(contact)).thenReturn(contactDto);

        // When
        VendorContactDto result = vendorContactService.getPrimaryContact(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isPrimary());
    }

    @Test
    void getPrimaryContact_notFound_throwsNotFoundException() {
        // Given
        when(vendorContactRepository.findByVendorIdAndPrimaryTrue(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorContactService.getPrimaryContact(999L));
    }

    @Test
    void getBillingContact_found_returnsDto() {
        // Given
        VendorContact billingContact = VendorContact.builder()
                .id(2L)
                .vendor(vendor)
                .name("Billing Person")
                .billingContact(true)
                .active(true)
                .build();
        VendorContactDto billingDto = VendorContactDto.builder()
                .id(2L)
                .vendorId(1L)
                .name("Billing Person")
                .billingContact(true)
                .build();

        when(vendorContactRepository.findByVendorIdAndBillingContactTrue(1L)).thenReturn(Optional.of(billingContact));
        when(vendorContactMapper.toDto(billingContact)).thenReturn(billingDto);

        // When
        VendorContactDto result = vendorContactService.getBillingContact(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isBillingContact());
    }

    @Test
    void getBillingContact_notFound_returnsNull() {
        // Given
        when(vendorContactRepository.findByVendorIdAndBillingContactTrue(1L)).thenReturn(Optional.empty());

        // When
        VendorContactDto result = vendorContactService.getBillingContact(1L);

        // Then
        assertNull(result);
    }

    @Test
    void getOrderingContact_found_returnsDto() {
        // Given
        VendorContact orderingContact = VendorContact.builder()
                .id(3L)
                .vendor(vendor)
                .name("Ordering Person")
                .orderingContact(true)
                .active(true)
                .build();
        VendorContactDto orderingDto = VendorContactDto.builder()
                .id(3L)
                .vendorId(1L)
                .name("Ordering Person")
                .orderingContact(true)
                .build();

        when(vendorContactRepository.findByVendorIdAndOrderingContactTrue(1L)).thenReturn(Optional.of(orderingContact));
        when(vendorContactMapper.toDto(orderingContact)).thenReturn(orderingDto);

        // When
        VendorContactDto result = vendorContactService.getOrderingContact(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isOrderingContact());
    }

    @Test
    void getOrderingContact_notFound_returnsNull() {
        // Given
        when(vendorContactRepository.findByVendorIdAndOrderingContactTrue(1L)).thenReturn(Optional.empty());

        // When
        VendorContactDto result = vendorContactService.getOrderingContact(1L);

        // Then
        assertNull(result);
    }

    @Test
    void createContact_success() {
        // Given
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Jane Smith")
                .email("jane@acme.com")
                .primary(false)
                .build();

        VendorContact newContact = VendorContact.builder()
                .id(2L)
                .name("Jane Smith")
                .email("jane@acme.com")
                .active(true)
                .build();

        VendorContactDto newDto = VendorContactDto.builder()
                .id(2L)
                .vendorId(1L)
                .name("Jane Smith")
                .email("jane@acme.com")
                .active(true)
                .build();

        when(vendorRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(vendor));
        when(vendorContactRepository.findByVendorIdAndEmail(1L, "jane@acme.com")).thenReturn(Optional.empty());
        when(vendorContactMapper.toEntity(request)).thenReturn(newContact);
        when(vendorContactRepository.save(any(VendorContact.class))).thenReturn(newContact);
        when(vendorContactMapper.toDto(newContact)).thenReturn(newDto);

        // When
        VendorContactDto result = vendorContactService.createContact(request);

        // Then
        assertNotNull(result);
        assertEquals("Jane Smith", result.getName());
        verify(vendorContactRepository).save(any(VendorContact.class));
    }

    @Test
    void createContact_vendorNotFound_throwsNotFoundException() {
        // Given
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(999L)
                .name("Jane Smith")
                .build();

        when(vendorRepository.findByIdAndTenantId(999L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorContactService.createContact(request));
        verify(vendorContactRepository, never()).save(any());
    }

    @Test
    void createContact_duplicateEmail_throwsBusinessException() {
        // Given
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Duplicate")
                .email("john@acme.com")
                .build();

        when(vendorRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(vendor));
        when(vendorContactRepository.findByVendorIdAndEmail(1L, "john@acme.com")).thenReturn(Optional.of(contact));

        // When/Then
        assertThrows(BusinessException.class, () -> vendorContactService.createContact(request));
        verify(vendorContactRepository, never()).save(any());
    }

    @Test
    void createContact_asPrimary_unsetsExistingPrimary() {
        // Given
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("New Primary")
                .primary(true)
                .build();

        VendorContact existingPrimary = VendorContact.builder()
                .id(10L)
                .vendor(vendor)
                .name("Old Primary")
                .primary(true)
                .active(true)
                .build();

        VendorContact newContact = VendorContact.builder()
                .id(2L)
                .name("New Primary")
                .primary(true)
                .active(true)
                .build();

        VendorContactDto newDto = VendorContactDto.builder()
                .id(2L)
                .name("New Primary")
                .primary(true)
                .build();

        when(vendorRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(vendor));
        when(vendorContactRepository.findByVendorIdAndPrimaryTrue(1L)).thenReturn(Optional.of(existingPrimary));
        when(vendorContactMapper.toEntity(request)).thenReturn(newContact);
        when(vendorContactRepository.save(any(VendorContact.class))).thenReturn(newContact);
        when(vendorContactMapper.toDto(newContact)).thenReturn(newDto);

        // When
        VendorContactDto result = vendorContactService.createContact(request);

        // Then
        assertNotNull(result);
        assertFalse(existingPrimary.isPrimary());
        verify(vendorContactRepository, times(2)).save(any(VendorContact.class));
    }

    @Test
    void updateContact_success() {
        // Given
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Updated Name")
                .email("john@acme.com")
                .primary(false)
                .build();

        when(vendorContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(vendorContactRepository.save(any(VendorContact.class))).thenReturn(contact);
        when(vendorContactMapper.toDto(contact)).thenReturn(contactDto);

        // When
        VendorContactDto result = vendorContactService.updateContact(1L, request);

        // Then
        assertNotNull(result);
        verify(vendorContactMapper).updateEntity(request, contact);
        verify(vendorContactRepository).save(any(VendorContact.class));
    }

    @Test
    void updateContact_notFound_throwsNotFoundException() {
        // Given
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Updated")
                .build();

        when(vendorContactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorContactService.updateContact(999L, request));
    }

    @Test
    void updateContact_duplicateEmail_throwsBusinessException() {
        // Given
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Updated")
                .email("other@acme.com")
                .build();

        when(vendorContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(vendorContactRepository.existsByVendorIdAndEmailAndIdNot(1L, "other@acme.com", 1L)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> vendorContactService.updateContact(1L, request));
        verify(vendorContactRepository, never()).save(any());
    }

    @Test
    void updateContact_setAsPrimary_unsetsExistingPrimary() {
        // Given
        VendorContact nonPrimaryContact = VendorContact.builder()
                .id(2L)
                .vendor(vendor)
                .name("Non-Primary")
                .email("np@acme.com")
                .primary(false)
                .active(true)
                .build();

        VendorContact existingPrimary = VendorContact.builder()
                .id(10L)
                .vendor(vendor)
                .name("Old Primary")
                .primary(true)
                .active(true)
                .build();

        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(1L)
                .name("Now Primary")
                .email("np@acme.com")
                .primary(true)
                .build();

        VendorContactDto updatedDto = VendorContactDto.builder()
                .id(2L)
                .name("Now Primary")
                .primary(true)
                .build();

        when(vendorContactRepository.findById(2L)).thenReturn(Optional.of(nonPrimaryContact));
        when(vendorContactRepository.findByVendorIdAndPrimaryTrue(1L)).thenReturn(Optional.of(existingPrimary));
        when(vendorContactRepository.save(any(VendorContact.class))).thenReturn(nonPrimaryContact);
        when(vendorContactMapper.toDto(nonPrimaryContact)).thenReturn(updatedDto);

        // When
        VendorContactDto result = vendorContactService.updateContact(2L, request);

        // Then
        assertNotNull(result);
        assertFalse(existingPrimary.isPrimary());
    }

    @Test
    void deleteContact_success_softDeletes() {
        // Given
        when(vendorContactRepository.findById(1L)).thenReturn(Optional.of(contact));

        // When
        vendorContactService.deleteContact(1L);

        // Then
        assertFalse(contact.isActive());
        verify(vendorContactRepository).save(contact);
    }

    @Test
    void deleteContact_notFound_throwsNotFoundException() {
        // Given
        when(vendorContactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorContactService.deleteContact(999L));
    }

    @Test
    void setPrimaryContact_success() {
        // Given
        VendorContact nonPrimaryContact = VendorContact.builder()
                .id(2L)
                .vendor(vendor)
                .name("To Be Primary")
                .primary(false)
                .active(true)
                .build();

        VendorContactDto primaryDto = VendorContactDto.builder()
                .id(2L)
                .name("To Be Primary")
                .primary(true)
                .build();

        when(vendorContactRepository.findById(2L)).thenReturn(Optional.of(nonPrimaryContact));
        when(vendorContactRepository.findByVendorIdAndPrimaryTrue(1L)).thenReturn(Optional.of(contact));
        when(vendorContactRepository.save(any(VendorContact.class))).thenReturn(nonPrimaryContact);
        when(vendorContactMapper.toDto(nonPrimaryContact)).thenReturn(primaryDto);

        // When
        VendorContactDto result = vendorContactService.setPrimaryContact(2L);

        // Then
        assertNotNull(result);
        assertTrue(nonPrimaryContact.isPrimary());
        assertFalse(contact.isPrimary());
    }

    @Test
    void setPrimaryContact_notFound_throwsNotFoundException() {
        // Given
        when(vendorContactRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorContactService.setPrimaryContact(999L));
    }
}
