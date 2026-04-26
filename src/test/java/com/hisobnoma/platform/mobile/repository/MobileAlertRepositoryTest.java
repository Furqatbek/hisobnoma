package com.hisobnoma.platform.mobile.repository;

import com.hisobnoma.platform.mobile.entity.MobileAlert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class MobileAlertRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MobileAlertRepository mobileAlertRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 100L;

    private MobileAlert persistAlert(Long userId, MobileAlert.AlertType type, String title,
                                      boolean read, boolean sentViaPush, Instant expiresAt) {
        MobileAlert a = MobileAlert.builder()
                .userId(userId).alertType(type).title(title).message("Test message")
                .priority(MobileAlert.AlertPriority.NORMAL)
                .read(read).sentViaPush(sentViaPush).expiresAt(expiresAt)
                .build();
        a.setTenantId(TENANT_ID);
        return em.persistAndFlush(a);
    }

    @Test
    void findByUserIdAndTenantIdOrderByCreatedAtDesc_returnsPagedResults() {
        persistAlert(USER_ID, MobileAlert.AlertType.LOW_STOCK, "Alert 1", false, false, null);
        persistAlert(USER_ID, MobileAlert.AlertType.PAYMENT_RECEIVED, "Alert 2", true, true, null);

        Page<MobileAlert> result = mobileAlertRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc_returnsUnreadOnly() {
        persistAlert(USER_ID, MobileAlert.AlertType.LOW_STOCK, "Unread", false, false, null);
        persistAlert(USER_ID, MobileAlert.AlertType.PAYMENT_RECEIVED, "Read", true, true, null);

        Page<MobileAlert> result = mobileAlertRepository.findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).isRead());
    }

    @Test
    void findByUserIdAndTenantIdAndAlertTypeOrderByCreatedAtDesc_filtersByType() {
        persistAlert(USER_ID, MobileAlert.AlertType.LOW_STOCK, "Low Stock", false, false, null);
        persistAlert(USER_ID, MobileAlert.AlertType.PAYMENT_RECEIVED, "Payment", false, false, null);

        Page<MobileAlert> result = mobileAlertRepository.findByUserIdAndTenantIdAndAlertTypeOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(MobileAlert.AlertType.LOW_STOCK, result.getContent().get(0).getAlertType());
    }

    @Test
    void findUnreadByUser_excludesExpiredAlerts() {
        Instant now = Instant.now();
        persistAlert(USER_ID, MobileAlert.AlertType.LOW_STOCK, "Valid Unread", false, false,
                now.plus(1, ChronoUnit.DAYS));
        persistAlert(USER_ID, MobileAlert.AlertType.SYSTEM_ALERT, "Expired Unread", false, false,
                now.minus(1, ChronoUnit.DAYS));
        persistAlert(USER_ID, MobileAlert.AlertType.PAYMENT_RECEIVED, "Read", true, true, null);
        persistAlert(USER_ID, MobileAlert.AlertType.ORDER_PLACED, "No Expiry Unread", false, false, null);

        List<MobileAlert> result = mobileAlertRepository.findUnreadByUser(USER_ID, TENANT_ID, now);

        assertEquals(2, result.size());
    }

    @Test
    void countUnreadByUser_returnsCorrectCount() {
        Instant now = Instant.now();
        persistAlert(USER_ID, MobileAlert.AlertType.LOW_STOCK, "Unread 1", false, false, null);
        persistAlert(USER_ID, MobileAlert.AlertType.SYSTEM_ALERT, "Unread 2", false, false,
                now.plus(1, ChronoUnit.DAYS));
        persistAlert(USER_ID, MobileAlert.AlertType.PAYMENT_RECEIVED, "Read", true, true, null);

        long result = mobileAlertRepository.countUnreadByUser(USER_ID, TENANT_ID, now);

        assertEquals(2L, result);
    }

    @Test
    void findUnsentPushAlerts_returnsUnsentAlertsForUser() {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        persistAlert(USER_ID, MobileAlert.AlertType.LOW_STOCK, "Unsent", false, false, null);
        persistAlert(USER_ID, MobileAlert.AlertType.PAYMENT_RECEIVED, "Sent", false, true, null);

        List<MobileAlert> result = mobileAlertRepository.findUnsentPushAlerts(USER_ID, since);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isSentViaPush());
    }

    @Test
    void findByUserIdAndTenantIdOrderByCreatedAtDesc_isolatesByUser() {
        persistAlert(USER_ID, MobileAlert.AlertType.LOW_STOCK, "User 100", false, false, null);
        persistAlert(200L, MobileAlert.AlertType.SYSTEM_ALERT, "User 200", false, false, null);

        Page<MobileAlert> result = mobileAlertRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                USER_ID, TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }
}
