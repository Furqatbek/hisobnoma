package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderLine;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class WebOrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WebOrderRepository repository;

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;

    private WebOrder newOrder;
    private WebOrder confirmedOrder;
    private WebOrder otherTenantOrder;

    @BeforeEach
    void setUp() {
        newOrder = persistOrder("WO-000001", WebOrderStatus.NEW, TENANT_ID);
        confirmedOrder = persistOrder("WO-000002", WebOrderStatus.CONFIRMED, TENANT_ID);
        otherTenantOrder = persistOrder("WO-000001", WebOrderStatus.NEW, OTHER_TENANT_ID);
    }

    private WebOrder persistOrder(String number, WebOrderStatus status, Long tenantId) {
        WebOrder order = WebOrder.builder()
                .orderNumber(number)
                .status(status)
                .customerName("Test Customer")
                .phone("+998901234567")
                .totalAmount(new BigDecimal("50000.0000"))
                .tenantId(tenantId)
                .build();
        order.addLine(WebOrderLine.builder()
                .productId(10L)
                .productName("Cola")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("50000.0000"))
                .lineTotal(new BigDecimal("50000.0000"))
                .tenantId(tenantId)
                .build());
        return entityManager.persistAndFlush(order);
    }

    @Test
    void sameOrderNumberAllowedAcrossTenantsButUniquePerTenant() {
        assertTrue(repository.existsByTenantIdAndOrderNumber(TENANT_ID, "WO-000001"));
        assertTrue(repository.existsByTenantIdAndOrderNumber(OTHER_TENANT_ID, "WO-000001"));
        assertFalse(repository.existsByTenantIdAndOrderNumber(TENANT_ID, "WO-999999"));
    }

    @Test
    void findByTenantIdAndOrderNumber_scopesByTenant() {
        assertEquals(newOrder.getId(),
                repository.findByTenantIdAndOrderNumber(TENANT_ID, "WO-000001").orElseThrow().getId());
        assertEquals(otherTenantOrder.getId(),
                repository.findByTenantIdAndOrderNumber(OTHER_TENANT_ID, "WO-000001").orElseThrow().getId());
    }

    @Test
    void countByTenantIdAndStatus_countsNewOrders() {
        assertEquals(1, repository.countByTenantIdAndStatus(TENANT_ID, WebOrderStatus.NEW));
        assertEquals(1, repository.countByTenantIdAndStatus(TENANT_ID, WebOrderStatus.CONFIRMED));
        assertEquals(0, repository.countByTenantIdAndStatus(TENANT_ID, WebOrderStatus.CANCELLED));
    }

    @Test
    void countByTenantIdAndCreatedAtAfter_countsRecentOrders() {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);

        assertEquals(2, repository.countByTenantIdAndCreatedAtAfter(TENANT_ID, yesterday));
        assertEquals(0, repository.countByTenantIdAndCreatedAtAfter(TENANT_ID, tomorrow));
    }

    @Test
    void findByTenantAndStatus_filtersByStatus() {
        Page<WebOrder> page = repository.findByTenantAndStatus(
                TENANT_ID, WebOrderStatus.NEW, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(newOrder.getId(), page.getContent().get(0).getId());
    }

    @Test
    void findAllByTenant_excludesOtherTenants() {
        Page<WebOrder> page = repository.findAllByTenant(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream()
                .noneMatch(o -> o.getId().equals(otherTenantOrder.getId())));
    }

    @Test
    void findRecentByTenant_limitsResults() {
        List<WebOrder> recent = repository.findRecentByTenant(TENANT_ID, PageRequest.of(0, 1));

        assertEquals(1, recent.size());
    }

    @Test
    void orderLinesArePersistedWithOrder() {
        WebOrder loaded = repository.findByIdAndTenantId(newOrder.getId(), TENANT_ID).orElseThrow();

        assertEquals(1, loaded.getLines().size());
        assertEquals("Cola", loaded.getLines().get(0).getProductName());
    }
}
