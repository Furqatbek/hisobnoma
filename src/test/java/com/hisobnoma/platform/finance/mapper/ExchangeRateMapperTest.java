package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateExchangeRateRequest;
import com.hisobnoma.platform.finance.dto.ExchangeRateDto;
import com.hisobnoma.platform.finance.entity.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeRateMapperTest {

    private ExchangeRateMapper exchangeRateMapper;

    @BeforeEach
    void setUp() {
        exchangeRateMapper = Mappers.getMapper(ExchangeRateMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        ExchangeRate entity = ExchangeRate.builder()
                .id(1L)
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12700.00000000"))
                .inverseRate(new BigDecimal("0.00007874"))
                .effectiveDate(LocalDate.of(2024, 6, 1))
                .expiryDate(LocalDate.of(2024, 6, 30))
                .source("Central Bank")
                .active(true)
                .notes("Monthly rate")
                .build();
        entity.setTenantId(1L);

        // When
        ExchangeRateDto dto = exchangeRateMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("USD", dto.getFromCurrency());
        assertEquals("UZS", dto.getToCurrency());
        assertEquals(new BigDecimal("12700.00000000"), dto.getRate());
        assertEquals(new BigDecimal("0.00007874"), dto.getInverseRate());
        assertEquals(LocalDate.of(2024, 6, 1), dto.getEffectiveDate());
        assertEquals("Central Bank", dto.getSource());
        assertTrue(dto.isActive());
    }

    @Test
    void toEntity_mapsRequestToEntity() {
        // Given
        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("EUR")
                .toCurrency("UZS")
                .rate(new BigDecimal("13500.00"))
                .effectiveDate(LocalDate.now())
                .source("Manual")
                .build();

        // When
        ExchangeRate entity = exchangeRateMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("EUR", entity.getFromCurrency());
        assertEquals("UZS", entity.getToCurrency());
        assertEquals(new BigDecimal("13500.00"), entity.getRate());
        assertNull(entity.getId());
        assertNull(entity.getInverseRate()); // inverse rate is ignored in mapping
    }

    @Test
    void toDtoList_mapsList() {
        // Given
        ExchangeRate r1 = ExchangeRate.builder().id(1L).fromCurrency("USD").toCurrency("UZS")
                .rate(new BigDecimal("12700")).effectiveDate(LocalDate.now()).build();
        ExchangeRate r2 = ExchangeRate.builder().id(2L).fromCurrency("EUR").toCurrency("UZS")
                .rate(new BigDecimal("13500")).effectiveDate(LocalDate.now()).build();

        // When
        List<ExchangeRateDto> dtos = exchangeRateMapper.toDtoList(List.of(r1, r2));

        // Then
        assertEquals(2, dtos.size());
    }

    @Test
    void updateEntity_updatesExistingEntity() {
        // Given
        ExchangeRate entity = ExchangeRate.builder()
                .id(1L)
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12700"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();

        CreateExchangeRateRequest request = CreateExchangeRateRequest.builder()
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12800"))
                .effectiveDate(LocalDate.of(2024, 2, 1))
                .build();

        // When
        exchangeRateMapper.updateEntity(request, entity);

        // Then
        assertEquals(new BigDecimal("12800"), entity.getRate());
        assertEquals(LocalDate.of(2024, 2, 1), entity.getEffectiveDate());
        assertEquals(1L, entity.getId());
    }
}
