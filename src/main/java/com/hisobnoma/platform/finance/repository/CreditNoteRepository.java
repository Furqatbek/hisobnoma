package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.CreditNote;
import com.hisobnoma.platform.finance.entity.CreditNoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    Optional<CreditNote> findByIdAndTenantId(Long id, Long tenantId);

    Optional<CreditNote> findByCreditNoteNumberAndTenantId(String creditNoteNumber, Long tenantId);

    Page<CreditNote> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<CreditNote> findByTenantIdAndCustomer_Id(Long tenantId, Long customerId, Pageable pageable);

    Page<CreditNote> findByTenantIdAndStatus(Long tenantId, CreditNoteStatus status, Pageable pageable);

    @Query("SELECT cn FROM CreditNote cn WHERE cn.tenantId = :tenantId AND cn.customer.id = :customerId " +
           "AND cn.status IN :statuses AND cn.balance > 0")
    List<CreditNote> findAvailableCreditNotesByCustomer(@Param("tenantId") Long tenantId,
                                                         @Param("customerId") Long customerId,
                                                         @Param("statuses") List<CreditNoteStatus> statuses);

    @Query("SELECT COALESCE(SUM(cn.balance), 0) FROM CreditNote cn " +
           "WHERE cn.tenantId = :tenantId AND cn.customer.id = :customerId " +
           "AND cn.status IN :statuses AND cn.balance > 0")
    BigDecimal sumAvailableCreditByCustomer(@Param("tenantId") Long tenantId,
                                            @Param("customerId") Long customerId,
                                            @Param("statuses") List<CreditNoteStatus> statuses);

    boolean existsByCreditNoteNumberAndTenantId(String creditNoteNumber, Long tenantId);

    List<CreditNote> findByOriginalInvoice_Id(Long originalInvoiceId);

    @Query("SELECT MAX(CAST(SUBSTRING(cn.creditNoteNumber, 4) AS integer)) FROM CreditNote cn " +
           "WHERE cn.tenantId = :tenantId AND cn.creditNoteNumber LIKE 'CN-%'")
    Integer findMaxCreditNoteNumber(@Param("tenantId") Long tenantId);
}
