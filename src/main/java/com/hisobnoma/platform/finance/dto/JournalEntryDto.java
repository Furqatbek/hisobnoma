package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.JournalSource;
import com.hisobnoma.platform.finance.entity.JournalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryDto {
    private Long id;
    private String entryNumber;
    private LocalDate entryDate;
    private Long fiscalPeriodId;
    private String fiscalPeriodName;
    private String description;
    private JournalStatus status;
    private JournalSource source;
    private String referenceType;
    private Long referenceId;
    private String referenceNumber;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String currency;
    private BigDecimal exchangeRate;
    private boolean recurring;
    private Long recurringTemplateId;
    private boolean reversing;
    private LocalDate reversalDate;
    private Long reversedEntryId;
    private Long reversingEntryId;
    private LocalDateTime postedAt;
    private Long postedBy;
    private LocalDateTime reversedAt;
    private Long reversedBy;
    private String notes;
    private String internalNotes;
    private String attachments;
    private boolean balanced;
    private List<JournalLineDto> lines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
