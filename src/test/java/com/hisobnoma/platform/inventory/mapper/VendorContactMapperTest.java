package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateVendorContactRequest;
import com.hisobnoma.platform.inventory.dto.VendorContactDto;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.entity.VendorContact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class VendorContactMapperTest {

    @Autowired
    private VendorContactMapper vendorContactMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Vendor vendor = Vendor.builder()
                .id(5L)
                .code("VND-001")
                .name("Tech Supplier")
                .active(true)
                .build();

        VendorContact contact = VendorContact.builder()
                .id(1L)
                .vendor(vendor)
                .name("Ali Karimov")
                .title("Sales Manager")
                .department("Sales")
                .email("ali@techsupplier.uz")
                .phone("+998901234567")
                .mobilePhone("+998931234567")
                .fax("+998711234567")
                .primary(true)
                .billingContact(false)
                .orderingContact(true)
                .notes("Primary contact for orders")
                .active(true)
                .build();

        // When
        VendorContactDto dto = vendorContactMapper.toDto(contact);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(5L, dto.getVendorId());
        assertEquals("Ali Karimov", dto.getName());
        assertEquals("Sales Manager", dto.getTitle());
        assertEquals("Sales", dto.getDepartment());
        assertEquals("ali@techsupplier.uz", dto.getEmail());
        assertEquals("+998901234567", dto.getPhone());
        assertEquals("+998931234567", dto.getMobilePhone());
        assertEquals("+998711234567", dto.getFax());
        assertTrue(dto.isPrimary());
        assertFalse(dto.isBillingContact());
        assertTrue(dto.isOrderingContact());
        assertEquals("Primary contact for orders", dto.getNotes());
        assertTrue(dto.isActive());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        Vendor vendor = Vendor.builder().id(1L).code("VND-001").name("Vendor").active(true).build();

        VendorContact c1 = VendorContact.builder()
                .id(1L).vendor(vendor).name("Contact A").active(true).build();
        VendorContact c2 = VendorContact.builder()
                .id(2L).vendor(vendor).name("Contact B").active(true).build();

        // When
        List<VendorContactDto> dtos = vendorContactMapper.toDtoList(List.of(c1, c2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Contact A", dtos.get(0).getName());
        assertEquals("Contact B", dtos.get(1).getName());
    }

    @Test
    void toEntity_mapsFromCreateRequest() {
        // Given
        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(5L)
                .name("Jane Smith")
                .title("Purchasing Director")
                .department("Purchasing")
                .email("jane@vendor.com")
                .phone("+998909876543")
                .mobilePhone("+998939876543")
                .fax("+998719876543")
                .primary(false)
                .billingContact(true)
                .orderingContact(false)
                .notes("Billing contact only")
                .build();

        // When
        VendorContact entity = vendorContactMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("Jane Smith", entity.getName());
        assertEquals("Purchasing Director", entity.getTitle());
        assertEquals("Purchasing", entity.getDepartment());
        assertEquals("jane@vendor.com", entity.getEmail());
        assertEquals("+998909876543", entity.getPhone());
        assertEquals("+998939876543", entity.getMobilePhone());
        assertEquals("+998719876543", entity.getFax());
        assertFalse(entity.isPrimary());
        assertTrue(entity.isBillingContact());
        assertFalse(entity.isOrderingContact());
        assertEquals("Billing contact only", entity.getNotes());
        // Active should be set to true (constant mapping)
        assertTrue(entity.isActive());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getVendor());
    }

    @Test
    void updateEntity_updatesFieldsOnExistingContact() {
        // Given
        Vendor vendor = Vendor.builder().id(5L).code("VND-001").name("Vendor").active(true).build();

        VendorContact existing = VendorContact.builder()
                .id(1L)
                .vendor(vendor)
                .name("Old Name")
                .email("old@vendor.com")
                .active(true)
                .build();

        CreateVendorContactRequest request = CreateVendorContactRequest.builder()
                .vendorId(5L)
                .name("Updated Name")
                .email("updated@vendor.com")
                .title("New Title")
                .build();

        // When
        vendorContactMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated Name", existing.getName());
        assertEquals("updated@vendor.com", existing.getEmail());
        assertEquals("New Title", existing.getTitle());
        // ID and vendor should remain unchanged
        assertEquals(1L, existing.getId());
        assertNotNull(existing.getVendor());
        assertEquals(5L, existing.getVendor().getId());
    }
}
