package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WebNotificationRepository extends JpaRepository<WebNotification, Long> {

    @Query("SELECT n FROM WebNotification n " +
           "WHERE n.tenantId = :tenantId AND n.webCustomerId = :customerId " +
           "ORDER BY n.createdAt DESC")
    Page<WebNotification> findByCustomer(@Param("tenantId") Long tenantId,
                                         @Param("customerId") Long customerId,
                                         Pageable pageable);

    @Query("SELECT COUNT(n) FROM WebNotification n " +
           "WHERE n.tenantId = :tenantId AND n.webCustomerId = :customerId AND n.read = false")
    long countUnread(@Param("tenantId") Long tenantId,
                     @Param("customerId") Long customerId);

    Optional<WebNotification> findByIdAndTenantIdAndWebCustomerId(Long id, Long tenantId, Long webCustomerId);

    @Modifying
    @Query("UPDATE WebNotification n SET n.read = true " +
           "WHERE n.tenantId = :tenantId AND n.webCustomerId = :customerId AND n.read = false")
    int markAllRead(@Param("tenantId") Long tenantId,
                    @Param("customerId") Long customerId);

    @Modifying
    @Query("DELETE FROM WebNotification n WHERE n.tenantId = :tenantId AND n.webCustomerId = :customerId")
    void deleteAllByTenantIdAndWebCustomerId(@Param("tenantId") Long tenantId,
                                             @Param("customerId") Long customerId);
}
