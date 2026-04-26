package com.hisobnoma.platform.sms.repository;

import com.hisobnoma.platform.sms.entity.SmsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class SmsTemplateRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SmsTemplateRepository smsTemplateRepository;

    private static final Long TENANT_ID = 1L;

    private SmsTemplate persistTemplate(String code, String name, String template, boolean active) {
        SmsTemplate t = SmsTemplate.builder()
                .code(code).name(name).template(template).active(active).build();
        t.setTenantId(TENANT_ID);
        return em.persistAndFlush(t);
    }

    @Test
    void findByTenantIdOrderByNameAsc_returnsOrderedList() {
        persistTemplate("WELCOME", "Welcome Message", "Welcome {name}!", true);
        persistTemplate("ORDER", "Order Confirmation", "Order {orderId} confirmed", true);
        persistTemplate("RESET", "Password Reset", "Your code is {code}", false);

        List<SmsTemplate> result = smsTemplateRepository.findByTenantIdOrderByNameAsc(TENANT_ID);

        assertEquals(3, result.size());
        assertEquals("Order Confirmation", result.get(0).getName());
        assertEquals("Password Reset", result.get(1).getName());
        assertEquals("Welcome Message", result.get(2).getName());
    }

    @Test
    void findByTenantIdAndActiveTrueOrderByNameAsc_returnsOnlyActive() {
        persistTemplate("WELCOME", "Welcome Message", "Welcome {name}!", true);
        persistTemplate("ORDER", "Order Confirmation", "Order {orderId} confirmed", true);
        persistTemplate("RESET", "Password Reset", "Your code is {code}", false);

        List<SmsTemplate> result = smsTemplateRepository.findByTenantIdAndActiveTrueOrderByNameAsc(TENANT_ID);

        assertEquals(2, result.size());
        result.forEach(t -> assertTrue(t.isActive()));
    }

    @Test
    void findByTenantIdAndId_existing_returnsTemplate() {
        SmsTemplate t = persistTemplate("WELCOME", "Welcome Message", "Welcome {name}!", true);

        Optional<SmsTemplate> result = smsTemplateRepository.findByTenantIdAndId(TENANT_ID, t.getId());

        assertTrue(result.isPresent());
        assertEquals("WELCOME", result.get().getCode());
    }

    @Test
    void findByTenantIdAndId_wrongTenant_returnsEmpty() {
        SmsTemplate t = persistTemplate("WELCOME", "Welcome Message", "Welcome {name}!", true);

        Optional<SmsTemplate> result = smsTemplateRepository.findByTenantIdAndId(999L, t.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTenantIdAndCode_existing_returnsTemplate() {
        persistTemplate("ORDER_CONF", "Order Confirmation", "Order {orderId} confirmed", true);

        Optional<SmsTemplate> result = smsTemplateRepository.findByTenantIdAndCode(TENANT_ID, "ORDER_CONF");

        assertTrue(result.isPresent());
        assertEquals("Order Confirmation", result.get().getName());
    }

    @Test
    void findByTenantIdAndCode_nonExistent_returnsEmpty() {
        Optional<SmsTemplate> result = smsTemplateRepository.findByTenantIdAndCode(TENANT_ID, "NONEXISTENT");

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByTenantIdAndCode_existing_returnsTrue() {
        persistTemplate("WELCOME", "Welcome Message", "Welcome {name}!", true);

        assertTrue(smsTemplateRepository.existsByTenantIdAndCode(TENANT_ID, "WELCOME"));
        assertFalse(smsTemplateRepository.existsByTenantIdAndCode(TENANT_ID, "NOPE"));
    }

    @Test
    void findByTenantIdOrderByNameAsc_multiTenant_isolatesData() {
        persistTemplate("T1", "Tenant 1 Template", "Hello T1", true);

        SmsTemplate t2Template = SmsTemplate.builder()
                .code("T2").name("Tenant 2 Template").template("Hello T2").active(true).build();
        t2Template.setTenantId(2L);
        em.persistAndFlush(t2Template);

        List<SmsTemplate> t1 = smsTemplateRepository.findByTenantIdOrderByNameAsc(TENANT_ID);
        List<SmsTemplate> t2 = smsTemplateRepository.findByTenantIdOrderByNameAsc(2L);

        assertEquals(1, t1.size());
        assertEquals("Tenant 1 Template", t1.get(0).getName());
        assertEquals(1, t2.size());
        assertEquals("Tenant 2 Template", t2.get(0).getName());
    }
}
