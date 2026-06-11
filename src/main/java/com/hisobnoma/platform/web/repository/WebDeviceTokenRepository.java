package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebDeviceTokenRepository extends JpaRepository<WebDeviceToken, Long> {

    Optional<WebDeviceToken> findByTenantIdAndToken(Long tenantId, String token);

    List<WebDeviceToken> findByTenantIdAndWebCustomerId(Long tenantId, Long webCustomerId);

    @Query("SELECT t FROM WebDeviceToken t WHERE t.tenantId = :tenantId AND t.webCustomerId IN :customerIds")
    List<WebDeviceToken> findByTenantIdAndWebCustomerIdIn(
            @Param("tenantId") Long tenantId,
            @Param("customerIds") List<Long> customerIds);

    @Modifying
    @Query("DELETE FROM WebDeviceToken t WHERE t.tenantId = :tenantId AND t.webCustomerId = :customerId AND t.token = :token")
    void deleteByTenantIdAndWebCustomerIdAndToken(
            @Param("tenantId") Long tenantId,
            @Param("customerId") Long customerId,
            @Param("token") String token);

    @Modifying
    @Query("DELETE FROM WebDeviceToken t WHERE t.tenantId = :tenantId AND t.webCustomerId = :customerId")
    void deleteAllByTenantIdAndWebCustomerId(
            @Param("tenantId") Long tenantId,
            @Param("customerId") Long customerId);

    long countByTenantIdAndWebCustomerId(Long tenantId, Long webCustomerId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
           "FROM WebDeviceToken t WHERE t.tenantId = :tenantId AND t.webCustomerId = :customerId")
    boolean existsByTenantIdAndWebCustomerId(
            @Param("tenantId") Long tenantId,
            @Param("customerId") Long customerId);
}
