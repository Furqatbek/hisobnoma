package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.ARPaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ARPaymentAllocationRepository extends JpaRepository<ARPaymentAllocation, Long> {

    List<ARPaymentAllocation> findByArPaymentId(Long arPaymentId);

    List<ARPaymentAllocation> findByArInvoiceId(Long arInvoiceId);

    List<ARPaymentAllocation> findByCreditNoteId(Long creditNoteId);

    @Query("SELECT COALESCE(SUM(a.allocatedAmount), 0) FROM ARPaymentAllocation a " +
           "WHERE a.arInvoice.id = :invoiceId")
    BigDecimal sumAllocatedAmountByInvoice(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(a.discountTaken), 0) FROM ARPaymentAllocation a " +
           "WHERE a.arInvoice.id = :invoiceId")
    BigDecimal sumDiscountTakenByInvoice(@Param("invoiceId") Long invoiceId);

    void deleteByArPaymentId(Long arPaymentId);

    int countByArPaymentId(Long arPaymentId);

    int countByArInvoiceId(Long arInvoiceId);
}
