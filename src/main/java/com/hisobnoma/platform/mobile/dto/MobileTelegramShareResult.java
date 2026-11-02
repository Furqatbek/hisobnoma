package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of sharing a report to Telegram. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileTelegramShareResult {
    /** "self" or "group". */
    private String target;
    /** How many linked Telegram chats were targeted. */
    private int recipients;
    /** How many were sent successfully. */
    private int sent;
}
