package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateFiscalYearRequest;
import com.hisobnoma.platform.finance.dto.FiscalPeriodDto;
import com.hisobnoma.platform.finance.dto.FiscalYearDto;
import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import com.hisobnoma.platform.finance.mapper.FiscalPeriodMapper;
import com.hisobnoma.platform.finance.mapper.FiscalYearMapper;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiscalPeriodServiceTest {

    @Mock
    private FiscalYearRepository fiscalYearRepository;

    @Mock
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Mock
    private FiscalYearMapper fiscalYearMapper;

    @Mock
    private FiscalPeriodMapper fiscalPeriodMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private FiscalPeriodService fiscalPeriodService;

    private FiscalYear fiscalYear;
    private FiscalYearDto fiscalYearDto;
    private FiscalPeriod fiscalPeriod;
    private FiscalPeriodDto fiscalPeriodDto;
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        fiscalYear = FiscalYear.builder()
                .id(1L)
                .year(2024)
                .name("Fiscal Year 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .status(PeriodStatus.OPEN)
                .current(true)
                .closed(false)
                .tenantId(TENANT_ID)
                .periods(new ArrayList<>())
                .build();

        fiscalYearDto = FiscalYearDto.builder()
                .id(1L)
                .year(2024)
                .name("Fiscal Year 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .status(PeriodStatus.OPEN)
                .current(true)
                .closed(false)
                .build();

        fiscalPeriod = FiscalPeriod.builder()
                .id(1L)
                .fiscalYear(fiscalYear)
                .periodNumber(1)
                .name("January")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .tenantId(TENANT_ID)
                .build();

        fiscalPeriodDto = FiscalPeriodDto.builder()
                .id(1L)
                .fiscalYearId(1L)
                .fiscalYear(2024)
                .periodNumber(1)
                .name("January")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .build();
    }

    // ====== getFiscalYears ======

    @Test
    void getFiscalYears_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<FiscalYear> page = new PageImpl<>(List.of(fiscalYear), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalYearRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(fiscalYearMapper.toDtoWithoutPeriods(fiscalYear)).thenReturn(fiscalYearDto);

        // When
        var result = fiscalPeriodService.getFiscalYears(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(2024, result.getContent().get(0).getYear());
    }

    // ====== createFiscalYear ======

    @Test
    void createFiscalYear_success() {
        // Given
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2024)
                .name("Fiscal Year 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .current(false)
                .generatePeriods(false)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalYearRepository.existsByYearAndTenantId(2024, TENANT_ID)).thenReturn(false);
        when(fiscalYearMapper.toEntity(request)).thenReturn(fiscalYear);
        when(fiscalYearRepository.save(any(FiscalYear.class))).thenReturn(fiscalYear);
        when(fiscalYearMapper.toDto(fiscalYear)).thenReturn(fiscalYearDto);

        // When
        FiscalYearDto result = fiscalPeriodService.createFiscalYear(request);

        // Then
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(PeriodStatus.OPEN, result.getStatus());
        verify(fiscalYearRepository).save(any(FiscalYear.class));
    }

    @Test
    void createFiscalYear_duplicateYear_throwsBusinessException() {
        // Given
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2024)
                .name("Fiscal Year 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalYearRepository.existsByYearAndTenantId(2024, TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.createFiscalYear(request));
        verify(fiscalYearRepository, never()).save(any());
    }

    @Test
    void createFiscalYear_invalidDates_throwsBusinessException() {
        // Given
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2024)
                .name("Fiscal Year 2024")
                .startDate(LocalDate.of(2024, 12, 31))
                .endDate(LocalDate.of(2024, 1, 1))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalYearRepository.existsByYearAndTenantId(2024, TENANT_ID)).thenReturn(false);

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.createFiscalYear(request));
    }

    // ====== closePeriod ======

    @Test
    void closeFiscalPeriod_open_becomesClosed() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalPeriodRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(fiscalPeriod));
        when(fiscalPeriodRepository.save(any(FiscalPeriod.class))).thenAnswer(inv -> inv.getArgument(0));

        FiscalPeriodDto closedDto = FiscalPeriodDto.builder()
                .id(1L)
                .status(PeriodStatus.CLOSED)
                .build();
        when(fiscalPeriodMapper.toDto(any(FiscalPeriod.class))).thenReturn(closedDto);

        // When
        FiscalPeriodDto result = fiscalPeriodService.closePeriod(1L);

        // Then
        assertNotNull(result);
        assertEquals(PeriodStatus.CLOSED, result.getStatus());
        verify(fiscalPeriodRepository).save(any(FiscalPeriod.class));
    }

    @Test
    void closeFiscalPeriod_alreadyClosed_throwsBusinessException() {
        // Given
        fiscalPeriod.setStatus(PeriodStatus.CLOSED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalPeriodRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(fiscalPeriod));

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.closePeriod(1L));
        verify(fiscalPeriodRepository, never()).save(any());
    }

    // ====== reopenPeriod ======

    @Test
    void reopenFiscalPeriod_closed_becomesOpen() {
        // Given
        fiscalPeriod.setStatus(PeriodStatus.CLOSED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalPeriodRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(fiscalPeriod));
        when(fiscalPeriodRepository.save(any(FiscalPeriod.class))).thenAnswer(inv -> inv.getArgument(0));

        FiscalPeriodDto reopenedDto = FiscalPeriodDto.builder()
                .id(1L)
                .status(PeriodStatus.OPEN)
                .build();
        when(fiscalPeriodMapper.toDto(any(FiscalPeriod.class))).thenReturn(reopenedDto);

        // When
        FiscalPeriodDto result = fiscalPeriodService.reopenPeriod(1L, "Correction needed");

        // Then
        assertNotNull(result);
        assertEquals(PeriodStatus.OPEN, result.getStatus());
    }

    @Test
    void reopenFiscalPeriod_locked_throwsBusinessException() {
        // Given
        fiscalPeriod.setStatus(PeriodStatus.LOCKED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalPeriodRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(fiscalPeriod));

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.reopenPeriod(1L, "reason"));
    }

    @Test
    void reopenFiscalPeriod_open_throwsBusinessException() {
        // Given - period is already OPEN, not CLOSED
        fiscalPeriod.setStatus(PeriodStatus.OPEN);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalPeriodRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(fiscalPeriod));

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.reopenPeriod(1L, "reason"));
    }

    @Test
    void reopenFiscalPeriod_closedFiscalYear_throwsBusinessException() {
        // Given
        fiscalPeriod.setStatus(PeriodStatus.CLOSED);
        fiscalYear.setClosed(true);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalPeriodRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(fiscalPeriod));

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.reopenPeriod(1L, "reason"));
    }

    // ====== getCurrentPeriod ======

    @Test
    void getCurrentFiscalPeriod_returnsOpenPeriod() {
        // Given
        LocalDate today = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalPeriodRepository.findByDateAndTenantId(today, TENANT_ID)).thenReturn(Optional.of(fiscalPeriod));
        when(fiscalPeriodMapper.toDto(fiscalPeriod)).thenReturn(fiscalPeriodDto);

        // When
        FiscalPeriodDto result = fiscalPeriodService.getPeriodByDate(today);

        // Then
        assertNotNull(result);
        assertEquals(PeriodStatus.OPEN, result.getStatus());
    }

    @Test
    void getCurrentFiscalPeriod_noOpenPeriod_throwsNotFoundException() {
        // Given
        LocalDate today = LocalDate.now();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalPeriodRepository.findByDateAndTenantId(today, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> fiscalPeriodService.getPeriodByDate(today));
    }

    // ====== closeYear ======

    @Test
    void closeFiscalYear_allPeriodsClosed_success() {
        // Given
        FiscalPeriod closedPeriod1 = FiscalPeriod.builder()
                .id(1L).periodNumber(1).status(PeriodStatus.CLOSED).fiscalYear(fiscalYear).build();
        FiscalPeriod closedPeriod2 = FiscalPeriod.builder()
                .id(2L).periodNumber(2).status(PeriodStatus.CLOSED).fiscalYear(fiscalYear).build();
        fiscalYear.getPeriods().add(closedPeriod1);
        fiscalYear.getPeriods().add(closedPeriod2);

        FiscalYearDto closedYearDto = FiscalYearDto.builder()
                .id(1L)
                .year(2024)
                .status(PeriodStatus.CLOSED)
                .closed(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalYearRepository.findByIdWithPeriods(1L)).thenReturn(Optional.of(fiscalYear));
        when(fiscalYearRepository.save(any(FiscalYear.class))).thenReturn(fiscalYear);
        when(fiscalYearMapper.toDto(any(FiscalYear.class))).thenReturn(closedYearDto);

        // When
        FiscalYearDto result = fiscalPeriodService.closeFiscalYear(1L);

        // Then
        assertNotNull(result);
        assertEquals(PeriodStatus.CLOSED, result.getStatus());
        assertTrue(result.isClosed());
    }

    @Test
    void closeFiscalYear_openPeriodsExist_throwsBusinessException() {
        // Given
        FiscalPeriod openPeriod = FiscalPeriod.builder()
                .id(1L).periodNumber(1).status(PeriodStatus.OPEN).fiscalYear(fiscalYear).build();
        fiscalYear.getPeriods().add(openPeriod);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalYearRepository.findByIdWithPeriods(1L)).thenReturn(Optional.of(fiscalYear));

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.closeFiscalYear(1L));
    }

    @Test
    void closeFiscalYear_alreadyClosed_throwsBusinessException() {
        // Given
        fiscalYear.setClosed(true);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(fiscalYearRepository.findByIdWithPeriods(1L)).thenReturn(Optional.of(fiscalYear));

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.closeFiscalYear(1L));
    }

    // ====== getFiscalPeriods ======

    @Test
    void getFiscalPeriods_returnsList() {
        // Given
        when(fiscalPeriodRepository.findByFiscalYearId(1L)).thenReturn(List.of(fiscalPeriod));
        when(fiscalPeriodMapper.toDtoList(List.of(fiscalPeriod))).thenReturn(List.of(fiscalPeriodDto));

        // When
        List<FiscalPeriodDto> result = fiscalPeriodService.getPeriodsByFiscalYear(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ====== getOpenPeriods ======

    @Test
    void getOpenPeriods_returnsOnlyOpen() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalPeriodRepository.findOpenPeriodsByTenantId(TENANT_ID)).thenReturn(List.of(fiscalPeriod));
        when(fiscalPeriodMapper.toDtoList(List.of(fiscalPeriod))).thenReturn(List.of(fiscalPeriodDto));

        // When
        List<FiscalPeriodDto> result = fiscalPeriodService.getOpenPeriods();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PeriodStatus.OPEN, result.get(0).getStatus());
    }

    // ====== validatePostingAllowed ======

    @Test
    void validatePostingAllowed_openPeriod_noException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalPeriodRepository.findByDateAndTenantId(any(LocalDate.class), eq(TENANT_ID)))
                .thenReturn(Optional.of(fiscalPeriod));

        // When/Then - no exception
        assertDoesNotThrow(() -> fiscalPeriodService.validatePostingAllowed(LocalDate.now()));
    }

    @Test
    void validatePostingAllowed_closedPeriod_throwsBusinessException() {
        // Given
        fiscalPeriod.setStatus(PeriodStatus.CLOSED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(fiscalPeriodRepository.findByDateAndTenantId(any(LocalDate.class), eq(TENANT_ID)))
                .thenReturn(Optional.of(fiscalPeriod));

        // When/Then
        assertThrows(BusinessException.class, () -> fiscalPeriodService.validatePostingAllowed(LocalDate.now()));
    }
}
