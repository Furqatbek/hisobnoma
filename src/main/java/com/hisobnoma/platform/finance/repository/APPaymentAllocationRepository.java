package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.APPaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface APPaymentAllocationRepository extends JpaRepository<APPaymentAllocation, Long> {

    List<APPaymentAllocation> findByApPaymentId(Long apPaymentId);

    List<APPaymentAllocation> findByApInvoiceId(Long apInvoiceId);

    @Query("SELECT COALESCE(SUM(a.allocatedAmount), 0) FROM APPaymentAllocation a " +
           "WHERE a.apInvoice.id = :invoiceId")
    BigDecimal sumAllocatedAmountByInvoice(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(a.discountTaken), 0) FROM APPaymentAllocation a " +
           "WHERE a.apInvoice.id = :invoiceId")
    BigDecimal sumDiscountTakenByInvoice(@Param("invoiceId") Long invoiceId);

    void deleteByApPaymentId(Long apPaymentId);

    int countByApPaymentId(Long apPaymentId);

    int countByApInvoiceId(Long apInvoiceId);
}
