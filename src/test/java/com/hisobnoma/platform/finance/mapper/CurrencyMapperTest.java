package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateCurrencyRequest;
import com.hisobnoma.platform.finance.dto.CurrencyDto;
import com.hisobnoma.platform.finance.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyMapperTest {

    private CurrencyMapper currencyMapper;

    @BeforeEach
    void setUp() {
        currencyMapper = Mappers.getMapper(CurrencyMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        Currency entity = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .symbol("$")
                .decimalPlaces(2)
                .baseCurrency(true)
                .active(true)
                .formatPattern("#,##0.00")
                .symbolPosition("BEFORE")
                .thousandsSeparator(",")
                .decimalSeparator(".")
                .sortOrder(1)
                .notes("Primary currency")
                .build();
        entity.setTenantId(1L);

        // When
        CurrencyDto dto = currencyMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("USD", dto.getCode());
        assertEquals("US Dollar", dto.getName());
        assertEquals("$", dto.getSymbol());
        assertEquals(2, dto.getDecimalPlaces());
        assertTrue(dto.isBaseCurrency());
        assertTrue(dto.isActive());
        assertEquals("#,##0.00", dto.getFormatPattern());
        assertEquals("BEFORE", dto.getSymbolPosition());
    }

    @Test
    void toEntity_mapsRequestToEntity() {
        // Given
        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("EUR")
                .name("Euro")
                .symbol("E")
                .decimalPlaces(2)
                .baseCurrency(false)
                .build();

        // When
        Currency entity = currencyMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("EUR", entity.getCode());
        assertEquals("Euro", entity.getName());
        assertEquals("E", entity.getSymbol());
        assertNull(entity.getId());
    }

    @Test
    void toDtoList_mapsList() {
        // Given
        Currency c1 = Currency.builder().id(1L).code("USD").name("US Dollar").build();
        Currency c2 = Currency.builder().id(2L).code("EUR").name("Euro").build();

        // When
        List<CurrencyDto> dtos = currencyMapper.toDtoList(List.of(c1, c2));

        // Then
        assertEquals(2, dtos.size());
        assertEquals("USD", dtos.get(0).getCode());
        assertEquals("EUR", dtos.get(1).getCode());
    }

    @Test
    void updateEntity_updatesExistingEntity() {
        // Given
        Currency entity = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .symbol("$")
                .build();

        CreateCurrencyRequest request = CreateCurrencyRequest.builder()
                .code("USD")
                .name("United States Dollar")
                .symbol("US$")
                .build();

        // When
        currencyMapper.updateEntity(request, entity);

        // Then
        assertEquals("United States Dollar", entity.getName());
        assertEquals("US$", entity.getSymbol());
        assertEquals(1L, entity.getId());
    }
}
