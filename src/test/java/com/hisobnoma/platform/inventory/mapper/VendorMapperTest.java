package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateVendorRequest;
import com.hisobnoma.platform.inventory.dto.VendorDto;
import com.hisobnoma.platform.inventory.entity.Vendor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class VendorMapperTest {

    @Autowired
    private VendorMapper vendorMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Vendor vendor = Vendor.builder()
                .id(1L)
                .tenantId(1L)
                .code("VND-001")
                .name("Tech Supplier Co.")
                .contactPerson("Ali Karimov")
                .email("ali@techsupplier.uz")
                .phone("+998901234567")
                .altPhone("+998901234568")
                .address("123 Tech Street")
                .city("Tashkent")
                .state("Tashkent")
                .country("Uzbekistan")
                .postalCode("100000")
                .taxId("UZ123456789")
                .paymentTerms("Net 30")
                .paymentTermsDays(30)
                .creditLimit(new BigDecimal("100000.0000"))
                .currentBalance(new BigDecimal("25000.0000"))
                .defaultCurrency("UZS")
                .bankName("National Bank")
                .bankAccount("12345678901234")
                .bankRouting("001234")
                .website("https://techsupplier.uz")
                .notes("Reliable supplier")
                .active(true)
                .preferred(true)
                .leadTimeDays(7)
                .minOrderAmount(new BigDecimal("500.0000"))
                .build();

        // When
        VendorDto dto = vendorMapper.toDto(vendor);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("VND-001", dto.getCode());
        assertEquals("Tech Supplier Co.", dto.getName());
        assertEquals("Ali Karimov", dto.getContactPerson());
        assertEquals("ali@techsupplier.uz", dto.getEmail());
        assertEquals("+998901234567", dto.getPhone());
        assertEquals("+998901234568", dto.getAltPhone());
        assertEquals("123 Tech Street", dto.getAddress());
        assertEquals("Tashkent", dto.getCity());
        assertEquals("Tashkent", dto.getState());
        assertEquals("Uzbekistan", dto.getCountry());
        assertEquals("100000", dto.getPostalCode());
        assertEquals("UZ123456789", dto.getTaxId());
        assertEquals("Net 30", dto.getPaymentTerms());
        assertEquals(30, dto.getPaymentTermsDays());
        assertEquals(new BigDecimal("100000.0000"), dto.getCreditLimit());
        assertEquals(new BigDecimal("25000.0000"), dto.getCurrentBalance());
        assertEquals("UZS", dto.getDefaultCurrency());
        assertEquals("National Bank", dto.getBankName());
        assertEquals("12345678901234", dto.getBankAccount());
        assertEquals("001234", dto.getBankRouting());
        assertEquals("https://techsupplier.uz", dto.getWebsite());
        assertEquals("Reliable supplier", dto.getNotes());
        assertTrue(dto.isActive());
        assertTrue(dto.isPreferred());
        assertEquals(7, dto.getLeadTimeDays());
        assertEquals(new BigDecimal("500.0000"), dto.getMinOrderAmount());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        Vendor v1 = Vendor.builder().id(1L).code("VND-001").name("Vendor A").active(true).build();
        Vendor v2 = Vendor.builder().id(2L).code("VND-002").name("Vendor B").active(false).build();

        // When
        List<VendorDto> dtos = vendorMapper.toDtoList(List.of(v1, v2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Vendor A", dtos.get(0).getName());
        assertEquals("Vendor B", dtos.get(1).getName());
    }

    @Test
    void toEntity_mapsFromCreateRequest() {
        // Given
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-003")
                .name("New Vendor")
                .contactPerson("Jane Doe")
                .email("jane@vendor.com")
                .phone("+998909876543")
                .address("456 Vendor Ave")
                .city("Bukhara")
                .country("Uzbekistan")
                .paymentTerms("Net 15")
                .paymentTermsDays(15)
                .creditLimit(new BigDecimal("50000.0000"))
                .defaultCurrency("USD")
                .active(true)
                .preferred(false)
                .leadTimeDays(5)
                .build();

        // When
        Vendor entity = vendorMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("VND-003", entity.getCode());
        assertEquals("New Vendor", entity.getName());
        assertEquals("Jane Doe", entity.getContactPerson());
        assertEquals("jane@vendor.com", entity.getEmail());
        assertEquals("+998909876543", entity.getPhone());
        assertEquals("456 Vendor Ave", entity.getAddress());
        assertEquals("Bukhara", entity.getCity());
        assertEquals("Uzbekistan", entity.getCountry());
        assertEquals("Net 15", entity.getPaymentTerms());
        assertEquals(15, entity.getPaymentTermsDays());
        assertEquals(new BigDecimal("50000.0000"), entity.getCreditLimit());
        assertEquals("USD", entity.getDefaultCurrency());
        assertTrue(entity.isActive());
        assertFalse(entity.isPreferred());
        assertEquals(5, entity.getLeadTimeDays());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
    }

    @Test
    void updateEntity_updatesFieldsOnExistingVendor() {
        // Given
        Vendor existing = Vendor.builder()
                .id(1L)
                .tenantId(1L)
                .code("VND-001")
                .name("Old Name")
                .active(true)
                .currentBalance(new BigDecimal("10000.0000"))
                .build();

        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-001")
                .name("Updated Vendor Name")
                .contactPerson("Updated Person")
                .email("updated@vendor.com")
                .active(true)
                .build();

        // When
        vendorMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated Vendor Name", existing.getName());
        assertEquals("Updated Person", existing.getContactPerson());
        assertEquals("updated@vendor.com", existing.getEmail());
        // ID, tenantId, and currentBalance should remain unchanged
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        assertEquals(new BigDecimal("10000.0000"), existing.getCurrentBalance());
    }
}
