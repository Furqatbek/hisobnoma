package com.hisobnoma.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated response wrapper class.
 * Provides a consistent structure for paginated API responses.
 *
 * @param <T> The type of elements in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private PageMetadata page;

    /**
     * Creates a PageResponse from a Spring Data Page object.
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return from(page);
    }

    /**
     * Creates a PageResponse from a Spring Data Page object.
     * Alias for of() method.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(PageMetadata.builder()
                        .number(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .first(page.isFirst())
                        .last(page.isLast())
                        .empty(page.isEmpty())
                        .build())
                .build();
    }

    /**
     * Creates a PageResponse from a list with pagination info.
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return PageResponse.<T>builder()
                .content(content)
                .page(PageMetadata.builder()
                        .number(page)
                        .size(size)
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .first(page == 0)
                        .last(page >= totalPages - 1)
                        .empty(content.isEmpty())
                        .build())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageMetadata {
        private int number;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean empty;
    }
}
