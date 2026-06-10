package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WebOrderRepository extends JpaRepository<WebOrder, Long> {

    Optional<WebOrder> findByIdAndTenantId(Long id, Long tenantId);

    Optional<WebOrder> findByTenantIdAndOrderNumber(Long tenantId, String orderNumber);

    boolean existsByTenantIdAndOrderNumber(Long tenantId, String orderNumber);

    long countByTenantIdAndStatus(Long tenantId, WebOrderStatus status);

    long countByTenantIdAndCreatedAtAfter(Long tenantId, Instant since);

    @Query("SELECT o FROM WebOrder o WHERE o.tenantId = :tenantId ORDER BY o.createdAt DESC")
    Page<WebOrder> findAllByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT o FROM WebOrder o WHERE o.tenantId = :tenantId AND o.status = :status ORDER BY o.createdAt DESC")
    Page<WebOrder> findByTenantAndStatus(@Param("tenantId") Long tenantId,
                                         @Param("status") WebOrderStatus status,
                                         Pageable pageable);

    @Query("SELECT o FROM WebOrder o WHERE o.tenantId = :tenantId ORDER BY o.createdAt DESC")
    List<WebOrder> findRecentByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT o FROM WebOrder o WHERE o.tenantId = :tenantId AND o.phoneNormalized = :phone " +
           "ORDER BY o.createdAt DESC")
    Page<WebOrder> findByTenantAndNormalizedPhone(@Param("tenantId") Long tenantId,
                                                  @Param("phone") String phone,
                                                  Pageable pageable);

    long countByTenantIdAndPhoneNormalized(Long tenantId, String phoneNormalized);
}
