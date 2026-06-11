package com.hisobnoma.platform.sms.util;

/**
 * Utility for normalizing phone numbers before sending to SMS providers.
 * DevSMS expects format: 998XXXXXXXXX (no '+' sign, with country code).
 */
public final class PhoneUtils {

    private static final String UZB_COUNTRY_CODE = "998";

    private PhoneUtils() {
    }

    /**
     * Normalizes a phone number to the format expected by DevSMS: 998XXXXXXXXX.
     * <ul>
     *   <li>Strips '+' prefix if present</li>
     *   <li>Strips leading '8' (local trunk prefix) and prepends 998</li>
     *   <li>Prepends 998 if the number is 9 digits (local number without country code)</li>
     * </ul>
     *
     * @param phone raw phone number
     * @return normalized phone in 998XXXXXXXXX format, or the original (stripped of non-digits) if unrecognized
     */
    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        // Remove everything except digits
        String digits = phone.replaceAll("[^0-9]", "");

        // 9 digits — local UZ number without country code (e.g. 901234567)
        if (digits.length() == 9 && !digits.startsWith(UZB_COUNTRY_CODE)) {
            return UZB_COUNTRY_CODE + digits;
        }

        // 10 digits starting with '8' — local trunk prefix (e.g. 8901234567)
        if (digits.length() == 10 && digits.startsWith("8")) {
            return UZB_COUNTRY_CODE + digits.substring(1);
        }

        // Already 12 digits with country code (e.g. 998901234567)
        return digits;
    }

    /**
     * Masks a phone number for logging — keeps only the last 4 digits
     * (e.g. "***4567") so logs never contain full numbers (PII).
     */
    public static String mask(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }
}
