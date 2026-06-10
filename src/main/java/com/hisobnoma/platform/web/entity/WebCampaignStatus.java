package com.hisobnoma.platform.web.entity;

/**
 * Lifecycle of a marketing campaign. A campaign is sendable only while DRAFT,
 * which makes a double-blast impossible.
 */
public enum WebCampaignStatus {
    DRAFT,
    SENDING,
    SENT,
    FAILED
}
