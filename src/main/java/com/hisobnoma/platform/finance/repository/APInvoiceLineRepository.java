package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.APInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface APInvoiceLineRepository extends JpaRepository<APInvoiceLine, Long> {

    List<APInvoiceLine> findByApInvoiceIdOrderByLineNumber(Long apInvoiceId);

    void deleteByApInvoiceId(Long apInvoiceId);

    int countByApInvoiceId(Long apInvoiceId);

    List<APInvoiceLine> findByPurchaseOrderLineId(Long purchaseOrderLineId);

    List<APInvoiceLine> findByReceivingLineId(Long receivingLineId);

    List<APInvoiceLine> findByProductId(Long productId);
}
