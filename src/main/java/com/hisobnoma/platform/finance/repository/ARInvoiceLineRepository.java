package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.ARInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ARInvoiceLineRepository extends JpaRepository<ARInvoiceLine, Long> {

    List<ARInvoiceLine> findByArInvoiceIdOrderByLineNumber(Long arInvoiceId);

    void deleteByArInvoiceId(Long arInvoiceId);

    int countByArInvoiceId(Long arInvoiceId);

    List<ARInvoiceLine> findByProductId(Long productId);

    List<ARInvoiceLine> findByPosLineId(Long posLineId);

    List<ARInvoiceLine> findBySalesOrderLineId(Long salesOrderLineId);
}
