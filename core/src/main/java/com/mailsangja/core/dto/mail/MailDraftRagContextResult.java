package com.mailsangja.core.dto.mail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MailDraftRagContextResult(
        List<MailDraftSearchContextResult> recentWrittenMessages,
        List<MailDraftSearchContextResult> relevantMessages,
        List<MailDraftSearchContextResult> threadMessages
) {

    public MailDraftRagContextResult {
        recentWrittenMessages = nullToEmpty(recentWrittenMessages);
        relevantMessages = nullToEmpty(relevantMessages);
        threadMessages = nullToEmpty(threadMessages);
    }

    public static MailDraftRagContextResult empty() {
        return new MailDraftRagContextResult(List.of(), List.of(), List.of());
    }

    public static MailDraftRagContextResult of(List<MailDraftSearchContextResult> recent, List<MailDraftSearchContextResult> relevant, List<MailDraftSearchContextResult> thread) {
        return new MailDraftRagContextResult(recent, relevant, thread);
    }

    public List<MailDraftSearchContextResult> referenceMessages() {
        List<MailDraftSearchContextResult> merged = new ArrayList<>();
        merged.addAll(threadMessages);
        merged.addAll(recentWrittenMessages);
        merged.addAll(relevantMessages);
        return deduplicateAndLimit(merged);
    }

    public List<MailDraftSearchContextResult> restoreTargetsFromThreadContext() {
        return List.of();
    }

    private static List<MailDraftSearchContextResult> nullToEmpty(List<MailDraftSearchContextResult> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private static List<MailDraftSearchContextResult> deduplicateAndLimit(List<MailDraftSearchContextResult> values) {
        Map<UUID, MailDraftSearchContextResult> unique = new LinkedHashMap<>();
        for (MailDraftSearchContextResult value : values) {
            unique.putIfAbsent(value.messageId(), value);
        }
        List<MailDraftSearchContextResult> limited = new ArrayList<>();
        for (MailDraftSearchContextResult value : unique.values()) {
            addIfNotFull(limited, value);
        }
        return limited;
    }

    private static void addIfNotFull(List<MailDraftSearchContextResult> values, MailDraftSearchContextResult value) {
        if (values.size() < 15) {
            values.add(value);
        }
    }
}
