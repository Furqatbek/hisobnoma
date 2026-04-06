package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import com.hisobnoma.platform.pos.dto.CashOperationRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "pos_cash_operations", indexes = {
    @Index(name = "idx_cash_ops_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cash_ops_shift", columnList = "shift_id"),
    @Index(name = "idx_cash_ops_created_at", columnList = "created_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CashOperation extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private CashOperationRequest.OperationType operationType;

    @Column(name = "amount", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "cashier_name", length = 100)
    private String cashierName;
}
