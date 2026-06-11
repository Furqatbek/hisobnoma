package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.web.dto.WebNotificationDto;
import com.hisobnoma.platform.web.entity.WebNotification;
import com.hisobnoma.platform.web.repository.WebNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebNotificationService {

    private final WebNotificationRepository notificationRepository;

    @Transactional
    public WebNotification create(Long tenantId, Long webCustomerId,
                                  String title, String body, String type,
                                  String referenceType, String referenceId) {
        return notificationRepository.save(WebNotification.builder()
                .tenantId(tenantId)
                .webCustomerId(webCustomerId)
                .title(title)
                .body(body)
                .type(type != null ? type : "GENERAL")
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<WebNotificationDto> getNotifications(Long tenantId, Long customerId, Pageable pageable) {
        return notificationRepository.findByCustomer(tenantId, customerId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long tenantId, Long customerId) {
        return notificationRepository.countUnread(tenantId, customerId);
    }

    @Transactional
    public boolean markRead(Long tenantId, Long customerId, Long notificationId) {
        return notificationRepository.findByIdAndTenantIdAndWebCustomerId(
                notificationId, tenantId, customerId)
                .map(n -> { n.setRead(true); return true; })
                .orElse(false);
    }

    @Transactional
    public int markAllRead(Long tenantId, Long customerId) {
        return notificationRepository.markAllRead(tenantId, customerId);
    }

    private WebNotificationDto toDto(WebNotification n) {
        return WebNotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .type(n.getType())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
