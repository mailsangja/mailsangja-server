package com.mailsangja.core.service.ai.draft;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MailDraftLexicalQueryBuilder {

    private static final int MAX_TERMS = 12;
    private static final Pattern TERM_PATTERN = Pattern.compile("[가-힣A-Za-z0-9]+");

    public String build(String query, Map<String, String> restoreTokenMap) {
        Set<String> terms = extractTerms(restoreTokens(query, restoreTokenMap));
        return String.join(" | ", terms);
    }

    private String restoreTokens(String query, Map<String, String> restoreTokenMap) {
        if (query == null || query.isBlank() || restoreTokenMap == null || restoreTokenMap.isEmpty()) {
            return nullToEmpty(query);
        }
        String restored = query;
        for (Map.Entry<String, String> entry : restoreTokenMap.entrySet()) {
            restored = restored.replace(entry.getKey(), nullToEmpty(entry.getValue()));
        }
        return restored;
    }

    private Set<String> extractTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        Matcher matcher = TERM_PATTERN.matcher(nullToEmpty(query));
        while (matcher.find() && terms.size() < MAX_TERMS) {
            addTerm(terms, matcher.group());
        }
        return terms;
    }

    private void addTerm(Set<String> terms, String value) {
        String normalized = normalize(value);
        if (normalized.length() >= 2) {
            terms.add(normalized);
        }
    }

    private String normalize(String value) {
        return nullToEmpty(value).trim().toLowerCase();
    }

    private String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
