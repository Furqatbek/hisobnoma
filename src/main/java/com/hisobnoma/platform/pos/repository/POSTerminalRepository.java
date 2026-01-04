package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.POSTerminal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface POSTerminalRepository extends JpaRepository<POSTerminal, Long> {

    @Query("SELECT t FROM POSTerminal t WHERE t.tenantId = :tenantId")
    Page<POSTerminal> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT t FROM POSTerminal t WHERE t.tenantId = :tenantId AND t.id = :id")
    Optional<POSTerminal> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM POSTerminal t WHERE t.tenantId = :tenantId AND t.terminalCode = :code")
    Optional<POSTerminal> findByTerminalCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM POSTerminal t WHERE t.tenantId = :tenantId AND t.location.id = :locationId")
    List<POSTerminal> findByLocationIdAndTenantId(@Param("locationId") Long locationId, @Param("tenantId") Long tenantId);

    @Query("SELECT t FROM POSTerminal t WHERE t.tenantId = :tenantId AND t.active = true")
    List<POSTerminal> findActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM POSTerminal t " +
           "WHERE t.tenantId = :tenantId AND t.terminalCode = :code AND (:excludeId IS NULL OR t.id != :excludeId)")
    boolean existsByTerminalCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId, @Param("excludeId") Long excludeId);

    @Query("SELECT t FROM POSTerminal t WHERE t.tenantId = :tenantId AND " +
           "(LOWER(t.terminalCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<POSTerminal> searchByTenantId(@Param("search") String search, @Param("tenantId") Long tenantId, Pageable pageable);
}
