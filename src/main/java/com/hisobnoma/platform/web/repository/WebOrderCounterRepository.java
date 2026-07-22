package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebOrderCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebOrderCounterRepository extends JpaRepository<WebOrderCounter, Long> {

    /** Locks the tenant's counter row (SELECT … FOR UPDATE) so allocation is serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WebOrderCounter> findByTenantId(Long tenantId);
}
