package com.forestguard.utils;

/**
 * Utility for Tunisian phone number handling.
 *
 * <p>Tunisia numbers are 8 digits, starting with 2, 4, 5, 7, or 9.
 * All numbers are stored and sent in E.164 format: {@code +216XXXXXXXX}.</p>
 */
public final class PhoneNumberUtils {

    /** Tunisian country code. */
    public static final String TN_PREFIX = "+216";

    private PhoneNumberUtils() {}

    /**
     * Normalizes any phone input to E.164 format ({@code +216XXXXXXXX}).
     *
     * <p>Accepts inputs like:
     * <ul>
     *   <li>{@code 54 770 867} → {@code +21654770867}</li>
     *   <li>{@code +21654770867} → {@code +21654770867}</li>
     *   <li>{@code 0021654770867} → {@code +21654770867}</li>
     * </ul>
     * </p>
     *
     * @param rawNumber raw input from the phone field (without the +216 prefix label)
     * @return E.164 number, or empty string if invalid
     */
    public static String normalizeToE164(String rawNumber) {
        if (rawNumber == null) return "";

        // Strip spaces, dashes, dots, parentheses
        String compact = rawNumber.trim().replaceAll("[\\s\\-().]", "");
        if (compact.isBlank()) return "";

        // Handle 00216... prefix
        if (compact.startsWith("00")) {
            compact = "+" + compact.substring(2);
        }

        // Already has + prefix
        if (compact.startsWith("+")) {
            String digits = compact.substring(1).replaceAll("\\D", "");
            return digits.matches("\\d{8,15}") ? "+" + digits : "";
        }

        // Strip any remaining non-digits
        String digits = compact.replaceAll("\\D", "");
        if (digits.isBlank()) return "";

        // 8 digits → assume Tunisia, prepend +216
        if (digits.length() == 8) {
            return isTunisianNumber(digits) ? TN_PREFIX + digits : "";
        }

        // 11 digits starting with 216 → already has country code without +
        if (digits.length() == 11 && digits.startsWith("216")) {
            String local = digits.substring(3);
            return isTunisianNumber(local) ? "+" + digits : "";
        }

        // Other international formats
        if (digits.length() >= 8 && digits.length() <= 15) {
            return "+" + digits;
        }

        return "";
    }

    /**
     * Validates that an 8-digit string is a valid Tunisian local number.
     * Valid first digits: 2, 4, 5, 7, 9.
     *
     * @param eightDigits exactly 8 digits, no prefix
     * @return {@code true} if valid Tunisian number
     */
    public static boolean isTunisianNumber(String eightDigits) {
        if (eightDigits == null) return false;
        String d = eightDigits.replaceAll("\\D", "");
        return d.matches("^[24579][0-9]{7}$");
    }

    /**
     * Formats 8 digits as {@code XX XXX XXX} for display.
     * Example: {@code "54770867"} → {@code "54 770 867"}
     *
     * @param eightDigits raw 8-digit string
     * @return formatted string, or original if not 8 digits
     */
    public static String formatDisplay(String eightDigits) {
        if (eightDigits == null) return "";
        String d = eightDigits.replaceAll("\\D", "");
        if (d.length() != 8) return eightDigits;
        return d.substring(0, 2) + " " + d.substring(2, 5) + " " + d.substring(5);
    }

    /**
     * Strips the {@code +216} prefix from a full E.164 number,
     * returning only the 8-digit local part for display in the input field.
     *
     * @param e164 full E.164 number (e.g. {@code +21654770867})
     * @return 8-digit local number, or original if not a Tunisian E.164 number
     */
    public static String toLocalDisplay(String e164) {
        if (e164 == null) return "";
        String s = e164.trim();
        if (s.startsWith(TN_PREFIX)) {
            return s.substring(TN_PREFIX.length());
        }
        if (s.startsWith("00216")) {
            return s.substring(5);
        }
        return s;
    }
}
