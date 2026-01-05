package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of an import operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDto {

    private int totalRows;
    private int successCount;
    private int errorCount;
    private int skippedCount;

    @Builder.Default
    private List<ImportErrorDto> errors = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportErrorDto {
        private int rowNumber;
        private String sku;
        private List<String> messages;
    }

    public void addError(int rowNumber, String sku, List<String> messages) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(ImportErrorDto.builder()
                .rowNumber(rowNumber)
                .sku(sku)
                .messages(messages)
                .build());
    }
}
