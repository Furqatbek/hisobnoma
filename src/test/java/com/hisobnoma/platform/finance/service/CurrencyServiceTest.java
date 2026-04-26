package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateCurrencyRequest;
import com.hisobnoma.platform.finance.dto.CurrencyDto;
import com.hisobnoma.platform.finance.entity.Currency;
import com.hisobnoma.platform.finance.mapper.CurrencyMapper;
import com.hisobnoma.platform.finance.repository.CurrencyRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CurrencyMapper currencyMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private CurrencyService currencyService;

    private Currency currency;
    private CurrencyDto currencyDto;
    private CreateCurrencyRequest createRequest;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        currency = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .symbol("$")
                .decimalPlaces(2)
                .baseCurrency(false)
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        currencyDto = CurrencyDto.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .symbol("$")
                .decimalPlaces(2)
                .baseCurrency(false)
                .active(true)
                .build();

        createRequest = CreateCurrencyRequest.builder()
                .code("USD")
                .name("US Dollar")
                .symbol("$")
                .decimalPlaces(2)
                .build();
    }

    // ====== getCurrencies ======

    @Test
    void getCurrencies_returnsList() {
        // Given
        Currency currency2 = Currency.builder().id(2L).code("EUR").name("Euro").tenantId(TENANT_ID).build();
        Currency currency3 = Currency.builder().id(3L).code("UZS").name("Uzbek Sum").tenantId(TENANT_ID).build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Currency> page = new PageImpl<>(List.of(currency, currency2, currency3), pageable, 3);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(currencyMapper.toDto(any(Currency.class))).thenReturn(currencyDto);

        // When
        var result = currencyService.getCurrencies(pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
    }

    // ====== getCurrency ======

    @Test
    void getCurrency_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(currency));
        when(currencyMapper.toDto(currency)).thenReturn(currencyDto);

        // When
        CurrencyDto result = currencyService.getCurrency(1L);

        // Then
        assertNotNull(result);
        assertEquals("USD", result.getCode());
    }

    @Test
    void getCurrency_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> currencyService.getCurrency(999L));
    }

    // ====== createCurrency ======

    @Test
    void createCurrency_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.existsByCodeAndTenantId("USD", TENANT_ID)).thenReturn(false);
        when(currencyRepository.existsByCodeAndTenantIdIsNull("USD")).thenReturn(false);
        when(currencyMapper.toEntity(createRequest)).thenReturn(currency);
        when(currencyRepository.save(any(Currency.class))).thenReturn(currency);
        when(currencyMapper.toDto(currency)).thenReturn(currencyDto);

        // When
        CurrencyDto result = currencyService.createCurrency(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("USD", result.getCode());
        verify(currencyRepository).save(any(Currency.class));
    }

    @Test
    void createCurrency_duplicateCode_throwsDuplicateResourceException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.existsByCodeAndTenantId("USD", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> currencyService.createCurrency(createRequest));
        verify(currencyRepository, never()).save(any());
    }

    @Test
    void createCurrency_duplicateSystemCode_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.existsByCodeAndTenantId("USD", TENANT_ID)).thenReturn(false);
        when(currencyRepository.existsByCodeAndTenantIdIsNull("USD")).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> currencyService.createCurrency(createRequest));
        verify(currencyRepository, never()).save(any());
    }

    // ====== setBaseCurrency ======

    @Test
    void setBaseCurrency_success() {
        // Given
        CurrencyDto baseCurrencyDto = CurrencyDto.builder()
                .id(1L)
                .code("USD")
                .baseCurrency(true)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findBaseCurrencyByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(currencyRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(currency));
        when(currencyRepository.save(any(Currency.class))).thenReturn(currency);
        when(currencyMapper.toDto(any(Currency.class))).thenReturn(baseCurrencyDto);

        // When
        CurrencyDto result = currencyService.setBaseCurrency(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isBaseCurrency());
    }

    @Test
    void setBaseCurrency_unsetsExistingBase() {
        // Given
        Currency existingBase = Currency.builder()
                .id(2L)
                .code("UZS")
                .baseCurrency(true)
                .tenantId(TENANT_ID)
                .build();

        CurrencyDto baseCurrencyDto = CurrencyDto.builder()
                .id(1L)
                .code("USD")
                .baseCurrency(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findBaseCurrencyByTenantId(TENANT_ID)).thenReturn(Optional.of(existingBase));
        when(currencyRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(currency));
        when(currencyRepository.save(any(Currency.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currencyMapper.toDto(any(Currency.class))).thenReturn(baseCurrencyDto);

        // When
        CurrencyDto result = currencyService.setBaseCurrency(1L);

        // Then
        assertNotNull(result);
        assertFalse(existingBase.isBaseCurrency());
        verify(currencyRepository, times(2)).save(any(Currency.class));
    }

    // ====== getBaseCurrency ======

    @Test
    void getBaseCurrency_returnsDefault() {
        // Given
        Currency baseCurrency = Currency.builder()
                .id(2L)
                .code("UZS")
                .name("Uzbek Sum")
                .baseCurrency(true)
                .tenantId(TENANT_ID)
                .build();

        CurrencyDto baseCurrencyDto = CurrencyDto.builder()
                .id(2L)
                .code("UZS")
                .name("Uzbek Sum")
                .baseCurrency(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findBaseCurrencyByTenantId(TENANT_ID)).thenReturn(Optional.of(baseCurrency));
        when(currencyMapper.toDto(baseCurrency)).thenReturn(baseCurrencyDto);

        // When
        CurrencyDto result = currencyService.getBaseCurrency();

        // Then
        assertNotNull(result);
        assertTrue(result.isBaseCurrency());
        assertEquals("UZS", result.getCode());
    }

    @Test
    void getBaseCurrency_noBase_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findBaseCurrencyByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> currencyService.getBaseCurrency());
    }

    // ====== getActiveCurrencies ======

    @Test
    void getActiveCurrencies_returnsOnlyActive() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(currency));
        when(currencyMapper.toDtoList(List.of(currency))).thenReturn(List.of(currencyDto));

        // When
        List<CurrencyDto> result = currencyService.getActiveCurrencies();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // ====== deactivateCurrency ======

    @Test
    void deactivateCurrency_baseCurrency_throwsBusinessException() {
        // Given
        currency.setBaseCurrency(true);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(currency));

        // When/Then
        assertThrows(BusinessException.class, () -> currencyService.deactivateCurrency(1L));
        verify(currencyRepository, never()).save(any());
    }

    // ====== updateCurrency ======

    @Test
    void updateCurrency_systemCurrency_throwsBusinessException() {
        // Given - system currency (tenantId == null)
        Currency systemCurrency = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .tenantId(null)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(currencyRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(systemCurrency));

        // When/Then
        assertThrows(BusinessException.class, () -> currencyService.updateCurrency(1L, createRequest));
    }
}
