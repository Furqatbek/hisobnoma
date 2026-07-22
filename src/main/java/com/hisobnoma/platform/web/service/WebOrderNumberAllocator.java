package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.web.entity.WebOrderCounter;
import com.hisobnoma.platform.web.repository.WebOrderCounterRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Allocates the next "WO-%06d" order number for a tenant from a counter row locked with
 * {@code SELECT … FOR UPDATE}, in its own short transaction. Concurrent checkouts therefore
 * serialize on allocation instead of both computing count(*)+1 and colliding on the unique
 * order-number index (which surfaced as a 500 to one of the customers).
 *
 * <p>Because allocation commits independently of the checkout transaction, a rolled-back checkout
 * leaves a gap in the sequence — which is fine; order numbers only need to be unique, not dense.
 * For a tenant with no counter row yet (created after the V80 seed), the counter is seeded from
 * the tenant's current order count. A first-use insert race between two concurrent checkouts is
 * resolved by the caller retrying once (each call is its own transaction, so the checkout
 * transaction is not poisoned by the losing insert).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebOrderNumberAllocator {

    private final WebOrderCounterRepository counterRepository;
    private final WebOrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(Long tenantId) {
        WebOrderCounter counter = counterRepository.findByTenantId(tenantId)
                .orElseGet(() -> new WebOrderCounter(tenantId,
                        orderRepository.countByTenantIdAndCreatedAtAfter(tenantId, Instant.EPOCH) + 1));
        long number = counter.getNextNumber();
        counter.setNextNumber(number + 1);
        counterRepository.saveAndFlush(counter);
        return String.format("WO-%06d", number);
    }
}
