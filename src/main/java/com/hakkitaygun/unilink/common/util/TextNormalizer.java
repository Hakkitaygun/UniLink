package com.hakkitaygun.unilink.common.util;

import java.util.Locale;

public final class TextNormalizer {

    public static final String TURKISH_CHARS = "İIıŞşĞğÜüÖöÇç";
    public static final String ASCII_CHARS = "iiissgguuoocc";

    private TextNormalizer() {
    }

    public static String fold(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            int index = TURKISH_CHARS.indexOf(c);
            result.append(index >= 0 ? ASCII_CHARS.charAt(index) : c);
        }
        return result.toString().replace("\u0307", "").toLowerCase(Locale.ROOT);
    }
}