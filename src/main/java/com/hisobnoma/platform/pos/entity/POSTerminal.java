package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import com.hisobnoma.platform.inventory.entity.Location;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * POS Terminal entity.
 * Represents a physical point of sale terminal/register.
 */
@Entity
@Table(name = "pos_terminals", indexes = {
    @Index(name = "idx_pos_terminals_tenant", columnList = "tenant_id"),
    @Index(name = "idx_pos_terminals_code", columnList = "terminal_code"),
    @Index(name = "idx_pos_terminals_location", columnList = "location_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class POSTerminal extends TenantAwareEntity {

    @Column(name = "terminal_code", nullable = false, length = 50)
    private String terminalCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "mac_address", length = 50)
    private String macAddress;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "printer_name", length = 100)
    private String printerName;

    @Column(name = "cash_drawer_enabled")
    @Builder.Default
    private boolean cashDrawerEnabled = true;

    @Column(name = "allow_offline")
    @Builder.Default
    private boolean allowOffline = false;

    @Column(name = "last_sync_at")
    private java.time.Instant lastSyncAt;

    @Column(name = "current_shift_id")
    private Long currentShiftId;
}
