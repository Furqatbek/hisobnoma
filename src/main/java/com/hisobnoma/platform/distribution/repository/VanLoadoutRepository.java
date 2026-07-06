package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.VanLoadout;
import com.hisobnoma.platform.distribution.entity.VanLoadoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VanLoadoutRepository extends JpaRepository<VanLoadout, Long> {

    @Query("SELECT l FROM VanLoadout l WHERE l.id = :id AND l.tenantId = :tenantId")
    Optional<VanLoadout> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT l FROM VanLoadout l WHERE l.tenantId = :tenantId")
    Page<VanLoadout> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT l FROM VanLoadout l WHERE l.tenantId = :tenantId AND l.status = :status")
    Page<VanLoadout> findByTenantIdAndStatus(@Param("tenantId") Long tenantId,
                                             @Param("status") VanLoadoutStatus status, Pageable pageable);

    @Query("SELECT l FROM VanLoadout l WHERE l.tenantId = :tenantId AND l.agentId = :agentId")
    Page<VanLoadout> findByTenantIdAndAgentId(@Param("tenantId") Long tenantId,
                                              @Param("agentId") Long agentId, Pageable pageable);

    boolean existsByTenantIdAndLoadoutNumber(Long tenantId, String loadoutNumber);

    @Query("SELECT MAX(l.loadoutNumber) FROM VanLoadout l " +
           "WHERE l.tenantId = :tenantId AND l.loadoutNumber LIKE CONCAT(:prefix, '%')")
    String findMaxLoadoutNumberByPrefix(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);
}
