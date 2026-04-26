package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateTaxCodeRequest;
import com.hisobnoma.platform.finance.dto.TaxCodeDto;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaxCodeMapperTest {

    private TaxCodeMapper taxCodeMapper;

    @BeforeEach
    void setUp() {
        taxCodeMapper = Mappers.getMapper(TaxCodeMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        Account salesAccount = Account.builder()
                .id(10L).code("2100").name("Sales Tax Payable").build();
        Account purchaseAccount = Account.builder()
                .id(11L).code("1200").name("Input Tax").build();

        TaxCode entity = TaxCode.builder()
                .id(1L)
                .code("VAT20")
                .name("VAT 20%")
                .description("Standard VAT rate")
                .taxType(TaxType.VAT)
                .inclusive(false)
                .compound(false)
                .appliesToSales(true)
                .appliesToPurchases(true)
                .salesAccount(salesAccount)
                .purchaseAccount(purchaseAccount)
                .taxAuthority("Tax Authority")
                .active(true)
                .systemCode(false)
                .sortOrder(1)
                .rates(new ArrayList<>())
                .build();
        entity.setTenantId(1L);

        // When
        TaxCodeDto dto = taxCodeMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("VAT20", dto.getCode());
        assertEquals("VAT 20%", dto.getName());
        assertEquals(TaxType.VAT, dto.getTaxType());
        assertFalse(dto.isInclusive());
        assertTrue(dto.isAppliesToSales());
        assertEquals(10L, dto.getSalesAccountId());
        assertEquals("2100", dto.getSalesAccountCode());
        assertEquals("Sales Tax Payable", dto.getSalesAccountName());
        assertEquals(11L, dto.getPurchaseAccountId());
        assertEquals("1200", dto.getPurchaseAccountCode());
    }

    @Test
    void toDtoWithoutRates_ratesIgnored() {
        // Given
        TaxCode entity = TaxCode.builder()
                .id(1L)
                .code("VAT20")
                .name("VAT 20%")
                .taxType(TaxType.VAT)
                .active(true)
                .rates(new ArrayList<>())
                .build();

        // When
        TaxCodeDto dto = taxCodeMapper.toDtoWithoutRates(entity);

        // Then
        assertNotNull(dto);
        assertEquals("VAT20", dto.getCode());
        assertNull(dto.getRates());
    }

    @Test
    void toEntity_mapsRequestToEntity() {
        // Given
        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("GST10")
                .name("GST 10%")
                .description("Goods and Services Tax")
                .taxType(TaxType.GST)
                .inclusive(true)
                .build();

        // When
        TaxCode entity = taxCodeMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("GST10", entity.getCode());
        assertEquals("GST 10%", entity.getName());
        assertEquals(TaxType.GST, entity.getTaxType());
        assertTrue(entity.isInclusive());
        assertNull(entity.getId());
    }

    @Test
    void toDtoList_mapsList() {
        // Given
        TaxCode tc1 = TaxCode.builder().id(1L).code("VAT20").name("VAT 20%")
                .taxType(TaxType.VAT).rates(new ArrayList<>()).build();
        TaxCode tc2 = TaxCode.builder().id(2L).code("VAT0").name("VAT Zero")
                .taxType(TaxType.VAT).rates(new ArrayList<>()).build();

        // When
        List<TaxCodeDto> dtos = taxCodeMapper.toDtoList(List.of(tc1, tc2));

        // Then
        assertEquals(2, dtos.size());
    }

    @Test
    void updateEntity_updatesExistingEntity() {
        // Given
        TaxCode entity = TaxCode.builder()
                .id(1L)
                .code("VAT20")
                .name("VAT 20%")
                .taxType(TaxType.VAT)
                .rates(new ArrayList<>())
                .build();

        CreateTaxCodeRequest request = CreateTaxCodeRequest.builder()
                .code("VAT20")
                .name("VAT 20% Updated")
                .taxType(TaxType.VAT)
                .description("Updated description")
                .build();

        // When
        taxCodeMapper.updateEntity(request, entity);

        // Then
        assertEquals("VAT 20% Updated", entity.getName());
        assertEquals("Updated description", entity.getDescription());
        assertEquals(1L, entity.getId());
    }
}
