package com.mailsangja.core.service.ai.label;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SnippetPreprocessor {

    private static final int MAX_LENGTH = 150;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("01[016789]-?\\d{3,4}-?\\d{4}");
    private static final Pattern RRN_PATTERN =
            Pattern.compile("\\d{6}-?[1-4]\\d{6}");
    private static final Pattern CARD_PATTERN =
            Pattern.compile("\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}");

    public String process(String snippet) {
        if (snippet == null) {
            return null;
        }
        String result = RRN_PATTERN.matcher(snippet).replaceAll("[RRN]");
        result = CARD_PATTERN.matcher(result).replaceAll("[CARD]");
        result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[PHONE]");
        return result.length() > MAX_LENGTH ? result.substring(0, MAX_LENGTH) : result;
    }
}
