package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateCreditNoteRequest;
import com.hisobnoma.platform.finance.dto.CreditNoteDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.mapper.CreditNoteMapper;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.CreditNoteRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditNoteServiceTest {

    @Mock
    private CreditNoteRepository creditNoteRepository;

    @Mock
    private ARInvoiceRepository arInvoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CreditNoteMapper creditNoteMapper;

    @Mock
    private CustomerService customerService;

    @Mock
    private GLIntegrationService glIntegrationService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private CreditNoteService creditNoteService;

    private Customer customer;
    private CreditNote creditNote;
    private CreditNoteDto creditNoteDto;
    private CreateCreditNoteRequest createRequest;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .code("CUST-001")
                .tenantId(TENANT_ID)
                .build();

        creditNote = CreditNote.builder()
                .id(1L)
                .creditNoteNumber("CN-000001")
                .customer(customer)
                .creditNoteDate(LocalDate.now())
                .status(CreditNoteStatus.DRAFT)
                .creditAmount(new BigDecimal("500.00"))
                .balance(new BigDecimal("500.00"))
                .appliedAmount(BigDecimal.ZERO)
                .refundedAmount(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();

        creditNoteDto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-000001")
                .customerId(1L)
                .creditNoteDate(LocalDate.now())
                .status(CreditNoteStatus.DRAFT)
                .balance(new BigDecimal("500.00"))
                .build();

        createRequest = CreateCreditNoteRequest.builder()
                .customerId(1L)
                .creditNoteDate(LocalDate.now())
                .creditAmount(new BigDecimal("500.00"))
                .reason("Defective goods returned")
                .build();
    }

    // ====== getCreditNotes ======

    @Test
    void getCreditNotes_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<CreditNote> page = new PageImpl<>(List.of(creditNote), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(creditNoteMapper.toDto(creditNote)).thenReturn(creditNoteDto);

        // When
        var result = creditNoteService.getCreditNotes(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("CN-000001", result.getContent().get(0).getCreditNoteNumber());
    }

    // ====== getCreditNote ======

    @Test
    void getCreditNote_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));
        when(creditNoteMapper.toDto(creditNote)).thenReturn(creditNoteDto);

        // When
        CreditNoteDto result = creditNoteService.getCreditNote(1L);

        // Then
        assertNotNull(result);
        assertEquals("CN-000001", result.getCreditNoteNumber());
    }

    @Test
    void getCreditNote_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> creditNoteService.getCreditNote(999L));
    }

    // ====== createCreditNote ======

    @Test
    void createCreditNote_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(creditNoteRepository.findMaxCreditNoteNumber(TENANT_ID)).thenReturn(null);
        when(creditNoteMapper.toEntity(createRequest)).thenReturn(creditNote);
        when(creditNoteRepository.save(any(CreditNote.class))).thenReturn(creditNote);
        when(creditNoteMapper.toDto(creditNote)).thenReturn(creditNoteDto);

        // When
        CreditNoteDto result = creditNoteService.createCreditNote(createRequest);

        // Then
        assertNotNull(result);
        assertEquals("CN-000001", result.getCreditNoteNumber());
        verify(creditNoteRepository).save(any(CreditNote.class));
    }

    @Test
    void createCreditNote_customerNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> creditNoteService.createCreditNote(createRequest));
        verify(creditNoteRepository, never()).save(any());
    }

    @Test
    void createCreditNote_withOriginalInvoice_success() {
        // Given
        ARInvoice invoice = ARInvoice.builder()
                .id(10L)
                .invoiceNumber("INV-001")
                .totalAmount(new BigDecimal("1000.00"))
                .customer(customer)
                .tenantId(TENANT_ID)
                .build();

        createRequest.setOriginalInvoiceId(10L);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(arInvoiceRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(creditNoteRepository.findByOriginalInvoice_Id(10L)).thenReturn(Collections.emptyList());
        when(creditNoteRepository.findMaxCreditNoteNumber(TENANT_ID)).thenReturn(null);
        when(creditNoteMapper.toEntity(createRequest)).thenReturn(creditNote);
        when(creditNoteRepository.save(any(CreditNote.class))).thenReturn(creditNote);
        when(creditNoteMapper.toDto(creditNote)).thenReturn(creditNoteDto);

        // When
        CreditNoteDto result = creditNoteService.createCreditNote(createRequest);

        // Then
        assertNotNull(result);
        verify(arInvoiceRepository).findByIdAndTenantId(10L, TENANT_ID);
    }

    @Test
    void createCreditNote_exceedsInvoiceAmount_throwsBusinessException() {
        // Given
        ARInvoice invoice = ARInvoice.builder()
                .id(10L)
                .invoiceNumber("INV-001")
                .totalAmount(new BigDecimal("200.00"))
                .customer(customer)
                .tenantId(TENANT_ID)
                .build();

        createRequest.setOriginalInvoiceId(10L);
        createRequest.setCreditAmount(new BigDecimal("500.00"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(arInvoiceRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(creditNoteRepository.findByOriginalInvoice_Id(10L)).thenReturn(Collections.emptyList());

        // When/Then
        assertThrows(BusinessException.class, () -> creditNoteService.createCreditNote(createRequest));
    }

    // ====== updateCreditNote ======

    @Test
    void updateCreditNote_draft_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));
        when(creditNoteRepository.save(any(CreditNote.class))).thenReturn(creditNote);
        when(creditNoteMapper.toDto(any(CreditNote.class))).thenReturn(creditNoteDto);

        // When
        CreditNoteDto result = creditNoteService.updateCreditNote(1L, createRequest);

        // Then
        assertNotNull(result);
        verify(creditNoteMapper).updateEntity(eq(createRequest), any(CreditNote.class));
    }

    @Test
    void updateCreditNote_notDraft_throwsBusinessException() {
        // Given
        creditNote.setStatus(CreditNoteStatus.APPROVED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));

        // When/Then
        assertThrows(BusinessException.class, () -> creditNoteService.updateCreditNote(1L, createRequest));
        verify(creditNoteRepository, never()).save(any());
    }

    // ====== approveCreditNote ======

    @Test
    void approveCreditNote_draft_success() {
        // Given
        CreditNoteDto approvedDto = CreditNoteDto.builder()
                .id(1L)
                .creditNoteNumber("CN-000001")
                .status(CreditNoteStatus.APPROVED)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));
        when(creditNoteRepository.save(any(CreditNote.class))).thenReturn(creditNote);
        when(creditNoteMapper.toDto(any(CreditNote.class))).thenReturn(approvedDto);

        // When
        CreditNoteDto result = creditNoteService.approveCreditNote(1L);

        // Then
        assertNotNull(result);
        assertEquals(CreditNoteStatus.APPROVED, result.getStatus());
        verify(glIntegrationService).postCreditNote(creditNote);
        verify(customerService).updateCustomerBalance(eq(1L), any(BigDecimal.class));
    }

    @Test
    void approveCreditNote_notDraft_throwsBusinessException() {
        // Given
        creditNote.setStatus(CreditNoteStatus.APPROVED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));

        // When/Then
        assertThrows(BusinessException.class, () -> creditNoteService.approveCreditNote(1L));
    }

    // ====== cancelCreditNote ======

    @Test
    void cancelCreditNote_draft_success() {
        // Given
        CreditNoteDto cancelledDto = CreditNoteDto.builder()
                .id(1L)
                .status(CreditNoteStatus.CANCELLED)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));
        when(creditNoteRepository.save(any(CreditNote.class))).thenReturn(creditNote);
        when(creditNoteMapper.toDto(any(CreditNote.class))).thenReturn(cancelledDto);

        // When
        CreditNoteDto result = creditNoteService.cancelCreditNote(1L, "No longer needed");

        // Then
        assertNotNull(result);
        assertEquals(CreditNoteStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelCreditNote_alreadyCancelled_throwsBusinessException() {
        // Given
        creditNote.setStatus(CreditNoteStatus.CANCELLED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));

        // When/Then
        assertThrows(BusinessException.class, () -> creditNoteService.cancelCreditNote(1L, "reason"));
    }

    @Test
    void cancelCreditNote_appliedToInvoice_throwsBusinessException() {
        // Given
        creditNote.setStatus(CreditNoteStatus.APPROVED);
        creditNote.setAppliedAmount(new BigDecimal("100.00"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));

        // When/Then
        assertThrows(BusinessException.class, () -> creditNoteService.cancelCreditNote(1L, "reason"));
    }

    @Test
    void cancelCreditNote_approved_reversesGLAndCustomerBalance() {
        // Given
        creditNote.setStatus(CreditNoteStatus.APPROVED);
        creditNote.setAppliedAmount(BigDecimal.ZERO);

        CreditNoteDto cancelledDto = CreditNoteDto.builder()
                .id(1L)
                .status(CreditNoteStatus.CANCELLED)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));
        when(creditNoteRepository.save(any(CreditNote.class))).thenReturn(creditNote);
        when(creditNoteMapper.toDto(any(CreditNote.class))).thenReturn(cancelledDto);

        // When
        CreditNoteDto result = creditNoteService.cancelCreditNote(1L, "Cancelled for reversal");

        // Then
        assertNotNull(result);
        verify(glIntegrationService).reverseCreditNote(eq(creditNote), eq("Cancelled for reversal"));
        verify(customerService).updateCustomerBalance(eq(1L), any(BigDecimal.class));
    }

    // ====== getCreditNotesByCustomer ======

    @Test
    void getCreditNotesByCustomer_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<CreditNote> page = new PageImpl<>(List.of(creditNote), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByTenantIdAndCustomer_Id(TENANT_ID, 1L, pageable)).thenReturn(page);
        when(creditNoteMapper.toDto(creditNote)).thenReturn(creditNoteDto);

        // When
        var result = creditNoteService.getCreditNotesByCustomer(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== getCreditNotesByStatus ======

    @Test
    void getCreditNotesByStatus_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<CreditNote> page = new PageImpl<>(List.of(creditNote), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByTenantIdAndStatus(TENANT_ID, CreditNoteStatus.DRAFT, pageable))
                .thenReturn(page);
        when(creditNoteMapper.toDto(creditNote)).thenReturn(creditNoteDto);

        // When
        var result = creditNoteService.getCreditNotesByStatus(CreditNoteStatus.DRAFT, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== applyToInvoice ======

    @Test
    void applyToInvoice_success() {
        // Given
        creditNote.setStatus(CreditNoteStatus.APPROVED);

        ARInvoice invoice = ARInvoice.builder()
                .id(10L)
                .invoiceNumber("INV-001")
                .customer(customer)
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .status(ARInvoiceStatus.SENT)
                .tenantId(TENANT_ID)
                .build();

        CreditNoteDto appliedDto = CreditNoteDto.builder()
                .id(1L)
                .status(CreditNoteStatus.APPLIED)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));
        when(arInvoiceRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(creditNoteRepository.save(any(CreditNote.class))).thenReturn(creditNote);
        when(creditNoteMapper.toDto(any(CreditNote.class))).thenReturn(appliedDto);

        // When
        CreditNoteDto result = creditNoteService.applyToInvoice(1L, 10L, new BigDecimal("500.00"));

        // Then
        assertNotNull(result);
        verify(arInvoiceRepository).save(invoice);
        verify(creditNoteRepository).save(any(CreditNote.class));
    }

    @Test
    void applyToInvoice_exceedsBalance_throwsBusinessException() {
        // Given
        creditNote.setStatus(CreditNoteStatus.APPROVED);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));

        // When/Then
        assertThrows(BusinessException.class,
                () -> creditNoteService.applyToInvoice(1L, 10L, new BigDecimal("600.00")));
    }

    @Test
    void applyToInvoice_differentCustomer_throwsBusinessException() {
        // Given
        creditNote.setStatus(CreditNoteStatus.APPROVED);

        Customer otherCustomer = Customer.builder().id(2L).code("CUST-002").tenantId(TENANT_ID).build();
        ARInvoice invoice = ARInvoice.builder()
                .id(10L)
                .customer(otherCustomer)
                .totalAmount(new BigDecimal("1000.00"))
                .balanceDue(new BigDecimal("1000.00"))
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(creditNoteRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(creditNote));
        when(arInvoiceRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(BusinessException.class,
                () -> creditNoteService.applyToInvoice(1L, 10L, new BigDecimal("500.00")));
    }
}
