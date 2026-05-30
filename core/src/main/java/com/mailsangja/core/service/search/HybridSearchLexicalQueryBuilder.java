package com.mailsangja.core.service.search;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HybridSearchLexicalQueryBuilder {

    private static final int MAX_TERMS = 12;
    private static final Pattern TERM_PATTERN = Pattern.compile("[가-힣A-Za-z0-9]+");

    public String build(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        Set<String> terms = new LinkedHashSet<>();
        Matcher matcher = TERM_PATTERN.matcher(query);
        while (matcher.find() && terms.size() < MAX_TERMS) {
            String term = matcher.group().toLowerCase();
            if (term.length() >= 2) {
                terms.add(term);
            }
        }
        return String.join(" | ", terms);
    }
}
