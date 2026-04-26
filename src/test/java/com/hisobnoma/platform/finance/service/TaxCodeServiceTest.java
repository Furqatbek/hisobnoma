package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateTaxCodeRequest;
import com.hisobnoma.platform.finance.dto.CreateTaxRateRequest;
import com.hisobnoma.platform.finance.dto.TaxCodeDto;
import com.hisobnoma.platform.finance.dto.TaxRateDto;
import com.hisobnoma.platform.finance.entity.Account;
import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxRate;
import com.hisobnoma.platform.finance.entity.TaxType;
import com.hisobnoma.platform.finance.mapper.TaxCodeMapper;
import com.hisobnoma.platform.finance.mapper.TaxRateMapper;
import com.hisobnoma.platform.finance.repository.AccountRepository;
import com.hisobnoma.platform.finance.repository.TaxCodeRepository;
import com.hisobnoma.platform.finance.repository.TaxRateRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxCodeServiceTest {

    @Mock
    private TaxCodeRepository taxCodeRepository;

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TaxCodeMapper taxCodeMapper;

    @Mock
    private TaxRateMapper taxRateMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private TaxCodeService taxCodeService;

    private TaxCode taxCode;
    private TaxCodeDto taxCodeDto;
    private CreateTaxCodeRequest createRequest;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        taxCode = TaxCode.builder()
                .id(1L)
                .code("VAT20")
                .name("VAT 20%")
                .description("Value Added Tax at 20%")
                .taxType(TaxType.VAT)
                .active(true)
                .systemCode(false)
                .tenantId(TENANT_ID)
                .rates(new ArrayList<>())
                .build();

        taxCodeDto = TaxCodeDto.builder()
                .id(1L)
                .code("VAT20")
                .name("VAT 20%")
                .description("Value Added Tax at 20%")
                .taxType(TaxType.VAT)
                .active(true)
                .systemCode(false)
                .build();

        createRequest = CreateTaxCodeRequest.builder()
                .code("VAT20")
                .name("VAT 20%")
                .description("Value Added Tax at 20%")
                .taxType(TaxType.VAT)
                .build();
    }

    // ====== getTaxCodes ======

    @Test
    void getTaxCodes_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxCode> page = new PageImpl<>(List.of(taxCode), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(taxCodeMapper.toDtoWithoutRates(taxCode)).thenReturn(taxCodeDto);

        // When
        var result = taxCodeService.getTaxCodes(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("VAT20", result.getContent().get(0).getCode());
    }

    // ====== createTaxCode ======

    @Test
    void createTaxCode_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.existsByCodeAndTenantId("VAT20", TENANT_ID)).thenReturn(false);
        when(taxCodeRepository.existsByCodeAndTenantIdIsNull("VAT20")).thenReturn(false);
        when(taxCodeMapper.toEntity(createRequest)).thenReturn(taxCode);
        when(taxCodeRepository.save(any(TaxCode.class))).thenReturn(taxCode);
        when(taxCodeMapper.toDto(taxCode)).thenReturn(taxCodeDto);

        // When
        TaxCodeDto result = taxCodeService.createTaxCode(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("VAT20", result.getCode());
        assertEquals(TaxType.VAT, result.getTaxType());
        verify(taxCodeRepository).save(any(TaxCode.class));
    }

    @Test
    void createTaxCode_duplicateCode_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.existsByCodeAndTenantId("VAT20", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> taxCodeService.createTaxCode(createRequest));
        verify(taxCodeRepository, never()).save(any());
    }

    @Test
    void createTaxCode_withGlAccounts_success() {
        // Given
        Account salesAccount = Account.builder().id(10L).code("2100").name("Sales Tax Payable").tenantId(TENANT_ID).build();
        Account purchaseAccount = Account.builder().id(11L).code("1200").name("Input Tax").tenantId(TENANT_ID).build();

        createRequest.setSalesAccountId(10L);
        createRequest.setPurchaseAccountId(11L);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.existsByCodeAndTenantId("VAT20", TENANT_ID)).thenReturn(false);
        when(taxCodeRepository.existsByCodeAndTenantIdIsNull("VAT20")).thenReturn(false);
        when(taxCodeMapper.toEntity(createRequest)).thenReturn(taxCode);
        when(accountRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(salesAccount));
        when(accountRepository.findByIdAndTenantId(11L, TENANT_ID)).thenReturn(Optional.of(purchaseAccount));
        when(taxCodeRepository.save(any(TaxCode.class))).thenReturn(taxCode);
        when(taxCodeMapper.toDto(taxCode)).thenReturn(taxCodeDto);

        // When
        TaxCodeDto result = taxCodeService.createTaxCode(createRequest);

        // Then
        assertNotNull(result);
        verify(accountRepository).findByIdAndTenantId(10L, TENANT_ID);
        verify(accountRepository).findByIdAndTenantId(11L, TENANT_ID);
    }

    // ====== updateTaxCode ======

    @Test
    void updateTaxCode_success() {
        // Given
        CreateTaxCodeRequest updateRequest = CreateTaxCodeRequest.builder()
                .code("VAT20")
                .name("VAT 20% Updated")
                .taxType(TaxType.VAT)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(taxCode));
        when(taxCodeRepository.save(any(TaxCode.class))).thenReturn(taxCode);
        when(taxCodeMapper.toDto(any(TaxCode.class))).thenReturn(taxCodeDto);

        // When
        TaxCodeDto result = taxCodeService.updateTaxCode(1L, updateRequest);

        // Then
        assertNotNull(result);
        verify(taxCodeMapper).updateEntity(eq(updateRequest), any(TaxCode.class));
        verify(taxCodeRepository).save(any(TaxCode.class));
    }

    @Test
    void updateTaxCode_systemCode_throwsBusinessException() {
        // Given
        taxCode.setSystemCode(true);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(taxCode));

        // When/Then
        assertThrows(BusinessException.class, () -> taxCodeService.updateTaxCode(1L, createRequest));
        verify(taxCodeRepository, never()).save(any());
    }

    @Test
    void updateTaxCode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> taxCodeService.updateTaxCode(999L, createRequest));
    }

    // ====== getActiveTaxCodes ======

    @Test
    void getActiveTaxCodes_returnsOnlyActive() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(taxCode));
        when(taxCodeMapper.toDtoList(List.of(taxCode))).thenReturn(List.of(taxCodeDto));

        // When
        List<TaxCodeDto> result = taxCodeService.getActiveTaxCodes();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // ====== getTaxCode ======

    @Test
    void getTaxCode_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByIdWithRatesAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(taxCode));
        when(taxCodeMapper.toDto(taxCode)).thenReturn(taxCodeDto);

        // When
        TaxCodeDto result = taxCodeService.getTaxCode(1L);

        // Then
        assertNotNull(result);
        assertEquals("VAT20", result.getCode());
    }

    @Test
    void getTaxCode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByIdWithRatesAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> taxCodeService.getTaxCode(999L));
    }

    // ====== deactivateTaxCode ======

    @Test
    void deactivateTaxCode_success() {
        // Given
        TaxCodeDto deactivatedDto = TaxCodeDto.builder().id(1L).code("VAT20").active(false).build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(taxCode));
        when(taxCodeRepository.save(any(TaxCode.class))).thenReturn(taxCode);
        when(taxCodeMapper.toDto(any(TaxCode.class))).thenReturn(deactivatedDto);

        // When
        TaxCodeDto result = taxCodeService.deactivateTaxCode(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.isActive());
    }

    @Test
    void deactivateTaxCode_systemCode_throwsBusinessException() {
        // Given
        taxCode.setSystemCode(true);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(taxCode));

        // When/Then
        assertThrows(BusinessException.class, () -> taxCodeService.deactivateTaxCode(1L));
    }

    // ====== addTaxRate ======

    @Test
    void addTaxRate_success() {
        // Given
        CreateTaxRateRequest rateRequest = CreateTaxRateRequest.builder()
                .taxCodeId(1L)
                .name("Standard Rate")
                .rate(new BigDecimal("20.00"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .build();

        TaxRate taxRate = TaxRate.builder()
                .id(1L)
                .taxCode(taxCode)
                .name("Standard Rate")
                .rate(new BigDecimal("20.00"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        TaxRateDto taxRateDto = TaxRateDto.builder()
                .id(1L)
                .taxCodeId(1L)
                .name("Standard Rate")
                .rate(new BigDecimal("20.00"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(taxCode));
        when(taxRateMapper.toEntity(rateRequest)).thenReturn(taxRate);
        when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);
        when(taxRateMapper.toDto(taxRate)).thenReturn(taxRateDto);

        // When
        TaxRateDto result = taxCodeService.addTaxRate(rateRequest);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("20.00"), result.getRate());
        verify(taxRateRepository).save(any(TaxRate.class));
    }

    // ====== getTaxCodesByType ======

    @Test
    void getTaxCodesByType_returnsFilteredCodes() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(taxCodeRepository.findByTaxTypeAndTenantId(TaxType.VAT, TENANT_ID)).thenReturn(List.of(taxCode));
        when(taxCodeMapper.toDtoList(List.of(taxCode))).thenReturn(List.of(taxCodeDto));

        // When
        List<TaxCodeDto> result = taxCodeService.getTaxCodesByType(TaxType.VAT);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TaxType.VAT, result.get(0).getTaxType());
    }
}
