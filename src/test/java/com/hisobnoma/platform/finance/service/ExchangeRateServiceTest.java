package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateExchangeRateRequest;
import com.hisobnoma.platform.finance.dto.ExchangeRateDto;
import com.hisobnoma.platform.finance.entity.ExchangeRate;
import com.hisobnoma.platform.finance.mapper.ExchangeRateMapper;
import com.hisobnoma.platform.finance.repository.ExchangeRateRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRate exchangeRate;
    private ExchangeRateDto exchangeRateDto;
    private CreateExchangeRateRequest createRequest;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        exchangeRate = ExchangeRate.builder()
                .id(1L)
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12700.00000000"))
                .inverseRate(new BigDecimal("0.00007874"))
                .effectiveDate(LocalDate.now())
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        exchangeRateDto = ExchangeRateDto.builder()
                .id(1L)
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12700.00000000"))
                .inverseRate(new BigDecimal("0.00007874"))
                .effectiveDate(LocalDate.now())
                .active(true)
                .build();

        createRequest = CreateExchangeRateRequest.builder()
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12700.00"))
                .effectiveDate(LocalDate.now())
                .source("Central Bank")
                .build();
    }

    // ====== getRates ======

    @Test
    void getRates_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ExchangeRate> page = new PageImpl<>(List.of(exchangeRate), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(exchangeRateMapper.toDto(exchangeRate)).thenReturn(exchangeRateDto);

        // When
        var result = exchangeRateService.getExchangeRates(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== getRate ======

    @Test
    void getRate_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(exchangeRate));
        when(exchangeRateMapper.toDto(exchangeRate)).thenReturn(exchangeRateDto);

        // When
        ExchangeRateDto result = exchangeRateService.getExchangeRate(1L);

        // Then
        assertNotNull(result);
        assertEquals("USD", result.getFromCurrency());
        assertEquals("UZS", result.getToCurrency());
    }

    @Test
    void getRate_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> exchangeRateService.getExchangeRate(999L));
    }

    // ====== createRate ======

    @Test
    void createRate_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateMapper.toEntity(createRequest)).thenReturn(exchangeRate);
        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenReturn(exchangeRate);
        when(exchangeRateMapper.toDto(exchangeRate)).thenReturn(exchangeRateDto);

        // When
        ExchangeRateDto result = exchangeRateService.createExchangeRate(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("12700.00000000"), result.getRate());
        verify(exchangeRateRepository).save(any(ExchangeRate.class));
    }

    @Test
    void createRate_sameCurrencyPair_throwsBusinessException() {
        // Given
        CreateExchangeRateRequest sameRequest = CreateExchangeRateRequest.builder()
                .fromCurrency("USD")
                .toCurrency("USD")
                .rate(BigDecimal.ONE)
                .effectiveDate(LocalDate.now())
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);

        // When/Then
        assertThrows(BusinessException.class, () -> exchangeRateService.createExchangeRate(sameRequest));
        verify(exchangeRateRepository, never()).save(any());
    }

    // ====== getLatestRate / getEffectiveRate ======

    @Test
    void getLatestRate_returnsEffectiveRate() {
        // Given
        LocalDate today = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findEffectiveRate(TENANT_ID, "USD", "UZS", today))
                .thenReturn(Optional.of(exchangeRate));
        when(exchangeRateMapper.toDto(exchangeRate)).thenReturn(exchangeRateDto);

        // When
        ExchangeRateDto result = exchangeRateService.getEffectiveRate("USD", "UZS", today);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("12700.00000000"), result.getRate());
    }

    @Test
    void getEffectiveRate_sameCurrency_returnsOne() {
        // Given
        LocalDate today = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);

        // When
        ExchangeRateDto result = exchangeRateService.getEffectiveRate("USD", "USD", today);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ONE, result.getRate());
        assertEquals(BigDecimal.ONE, result.getInverseRate());
    }

    @Test
    void getEffectiveRate_notFound_throwsNotFoundException() {
        // Given
        LocalDate today = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findEffectiveRate(TENANT_ID, "XYZ", "UZS", today))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
                () -> exchangeRateService.getEffectiveRate("XYZ", "UZS", today));
    }

    // ====== getRateForDate ======

    @Test
    void getRateForDate_returnsMatchingRates() {
        // Given
        LocalDate date = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findByEffectiveDate(TENANT_ID, date)).thenReturn(List.of(exchangeRate));
        when(exchangeRateMapper.toDtoList(List.of(exchangeRate))).thenReturn(List.of(exchangeRateDto));

        // When
        List<ExchangeRateDto> result = exchangeRateService.getRatesByDate(date);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ====== getRatesByPair ======

    @Test
    void getRatesByPair_returnsHistoricalRates() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(TENANT_ID, "USD", "UZS"))
                .thenReturn(List.of(exchangeRate));
        when(exchangeRateMapper.toDtoList(List.of(exchangeRate))).thenReturn(List.of(exchangeRateDto));

        // When
        List<ExchangeRateDto> result = exchangeRateService.getRatesByCurrencyPair("USD", "UZS");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("USD", result.get(0).getFromCurrency());
        assertEquals("UZS", result.get(0).getToCurrency());
    }

    // ====== convert ======

    @Test
    void convert_correctResult() {
        // Given
        LocalDate today = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findEffectiveRate(TENANT_ID, "USD", "UZS", today))
                .thenReturn(Optional.of(exchangeRate));
        when(exchangeRateMapper.toDto(exchangeRate)).thenReturn(exchangeRateDto);

        // When
        BigDecimal result = exchangeRateService.convert(new BigDecimal("100"), "USD", "UZS", today);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("1270000.00000000"), result);
    }

    @Test
    void convert_sameCurrency_returnsOriginal() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");

        // When
        BigDecimal result = exchangeRateService.convert(amount, "UZS", "UZS", LocalDate.now());

        // Then
        assertEquals(amount, result);
    }

    @Test
    void convert_nullAmount_returnsNull() {
        // When
        BigDecimal result = exchangeRateService.convert(null, "USD", "UZS", LocalDate.now());

        // Then
        assertNull(result);
    }

    // ====== updateExchangeRate ======

    @Test
    void updateRate_success() {
        // Given
        CreateExchangeRateRequest updateRequest = CreateExchangeRateRequest.builder()
                .fromCurrency("USD")
                .toCurrency("UZS")
                .rate(new BigDecimal("12800.00"))
                .effectiveDate(LocalDate.now())
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(exchangeRate));
        when(exchangeRateRepository.save(any(ExchangeRate.class))).thenReturn(exchangeRate);
        when(exchangeRateMapper.toDto(any(ExchangeRate.class))).thenReturn(exchangeRateDto);

        // When
        ExchangeRateDto result = exchangeRateService.updateExchangeRate(1L, updateRequest);

        // Then
        assertNotNull(result);
        verify(exchangeRateMapper).updateEntity(eq(updateRequest), any(ExchangeRate.class));
        verify(exchangeRateRepository).save(any(ExchangeRate.class));
    }

    @Test
    void updateRate_sameCurrency_throwsBusinessException() {
        // Given
        CreateExchangeRateRequest sameRequest = CreateExchangeRateRequest.builder()
                .fromCurrency("USD")
                .toCurrency("USD")
                .rate(BigDecimal.ONE)
                .effectiveDate(LocalDate.now())
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(exchangeRate));

        // When/Then
        assertThrows(BusinessException.class, () -> exchangeRateService.updateExchangeRate(1L, sameRequest));
    }

    // ====== deleteExchangeRate ======

    @Test
    void deleteRate_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(exchangeRate));

        // When
        exchangeRateService.deleteExchangeRate(1L);

        // Then
        verify(exchangeRateRepository).delete(exchangeRate);
    }

    @Test
    void deleteRate_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(exchangeRateRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> exchangeRateService.deleteExchangeRate(999L));
    }
}
