package com.mailsangja.worker.handler.label;

import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 라벨 규칙을 컴파일하고 메시지 목록에 대해 라벨을 평가하는 컴포넌트.
 *
 * 평가 전략:
 * 1. 활성 라벨 규칙을 ConditionRef 단위로 컴파일하여 필드별 인덱스를 생성한다.
 * 2. EQUALS/BOOLEAN 조건은 Map 인덱스(O(1))로 후보를 추출한다.
 * 3. CONTAINS/NOT_CONTAINS 조건은 Aho-Corasick automaton으로 후보를 추출한다.
 * 4. 후보 라벨에 한해 전체 조건(DNF: OR of AND)을 정밀 평가한다.
 * 5. 결과는 메시지 ID → 적용할 Label 목록으로 반환한다.
 */
@Slf4j
@Component
public class LabelRuleCompiler {

    private static final int MAX_VALUE_LENGTH = 200;

    /**
     * 라벨 규칙을 컴파일하여 메시지별 적용 라벨 맵을 반환한다.
     *
     * @param labels                    사용자의 활성 라벨 목록
     * @param messages                  평가 대상 메시지 목록 (messageLabels 컬렉션 초기화 필요)
     * @param messageIdsWithAttachments 첨부파일이 있는 메시지 ID 집합 (별도 쿼리로 조회)
     * @return 메시지 ID → 적용할 Label 목록
     */
    public Map<UUID, List<Label>> compile(List<Label> labels, List<Message> messages, Set<UUID> messageIdsWithAttachments) {
        if (labels.isEmpty() || messages.isEmpty()) {
            return Map.of();
        }

        CompiledRules compiled = buildIndex(labels);
        Map<UUID, List<Label>> result = new HashMap<>();

        for (Message message : messages) {
            boolean hasAttachment = messageIdsWithAttachments.contains(message.getId());
            List<Label> matchedLabels = evaluate(message, compiled, labels, hasAttachment);
            result.put(message.getId(), matchedLabels);
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule compilation
    // ─────────────────────────────────────────────────────────────────────────

    private CompiledRules buildIndex(List<Label> labels) {
        Map<String, List<ConditionRef>> equalsIndex = new HashMap<>();
        Map<String, List<ConditionRef>> containsIndex = new HashMap<>();
        List<ConditionRef> notContainsRefs = new ArrayList<>();
        Set<UUID> notContainsLabelIds = new HashSet<>();
        Map<UUID, List<ParsedGroup>> parsedGroupsByLabel = new HashMap<>();

        AhoCorasickAutomaton subjectAutomaton = new AhoCorasickAutomaton();
        AhoCorasickAutomaton bodyTextAutomaton = new AhoCorasickAutomaton();
        AhoCorasickAutomaton fromAddressAutomaton = new AhoCorasickAutomaton();
        AhoCorasickAutomaton fromDomainAutomaton = new AhoCorasickAutomaton();
        AhoCorasickAutomaton toAddressAutomaton = new AhoCorasickAutomaton();
        AhoCorasickAutomaton ccAddressAutomaton = new AhoCorasickAutomaton();

        for (Label label : labels) {
            if (label.getRule() == null) {
                continue;
            }

            List<ParsedGroup> parsedGroups = parseGroups(label);
            if (parsedGroups.isEmpty()) {
                continue;
            }
            parsedGroupsByLabel.put(label.getId(), parsedGroups);

            for (ParsedGroup group : parsedGroups) {
                for (ParsedCondition cond : group.conditions()) {
                    String field = cond.field();
                    String operator = cond.operator();
                    String value = cond.normalizedValue();
                    ConditionRef ref = new ConditionRef(label.getId(), group.groupId(), cond.conditionId(), field, operator, value);

                    switch (operator) {
                        case "EQUALS", "BOOLEAN" -> {
                            String indexKey = field + ":" + (value != null ? value : "true");
                            equalsIndex.computeIfAbsent(indexKey, k -> new ArrayList<>()).add(ref);
                        }
                        case "CONTAINS" -> {
                            if (value != null && !value.isBlank()) {
                                containsIndex.computeIfAbsent(value, k -> new ArrayList<>()).add(ref);
                                addKeywordToAutomaton(field, value, subjectAutomaton, bodyTextAutomaton,
                                        fromAddressAutomaton, fromDomainAutomaton, toAddressAutomaton, ccAddressAutomaton);
                            }
                        }
                        case "NOT_CONTAINS" -> {
                            notContainsRefs.add(ref);
                            notContainsLabelIds.add(label.getId());
                        }
                        default -> { /* validated upstream */ }
                    }
                }
            }
        }

        subjectAutomaton.build();
        bodyTextAutomaton.build();
        fromAddressAutomaton.build();
        fromDomainAutomaton.build();
        toAddressAutomaton.build();
        ccAddressAutomaton.build();

        return new CompiledRules(
                equalsIndex,
                containsIndex,
                notContainsRefs,
                notContainsLabelIds,
                parsedGroupsByLabel,
                subjectAutomaton,
                bodyTextAutomaton,
                fromAddressAutomaton,
                fromDomainAutomaton,
                toAddressAutomaton,
                ccAddressAutomaton
        );
    }

    private void addKeywordToAutomaton(
            String field,
            String keyword,
            AhoCorasickAutomaton subjectAutomaton,
            AhoCorasickAutomaton bodyTextAutomaton,
            AhoCorasickAutomaton fromAddressAutomaton,
            AhoCorasickAutomaton fromDomainAutomaton,
            AhoCorasickAutomaton toAddressAutomaton,
            AhoCorasickAutomaton ccAddressAutomaton
    ) {
        switch (field) {
            case "SUBJECT"       -> subjectAutomaton.addPattern(keyword);
            case "BODY_TEXT"     -> bodyTextAutomaton.addPattern(keyword);
            case "FROM_ADDRESS"  -> fromAddressAutomaton.addPattern(keyword);
            case "FROM_DOMAIN"   -> fromDomainAutomaton.addPattern(keyword);
            case "TO_ADDRESS"    -> toAddressAutomaton.addPattern(keyword);
            case "CC_ADDRESS"    -> ccAddressAutomaton.addPattern(keyword);
            default              -> { /* not used for automaton */ }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Message evaluation
    // ─────────────────────────────────────────────────────────────────────────

    private List<Label> evaluate(Message message, CompiledRules compiled, List<Label> allLabels, boolean hasAttachment) {
        MessageFeatures features = extractFeatures(message, hasAttachment);

        Map<UUID, Map<String, Set<String>>> matchedCondIdsByLabelByGroup = new HashMap<>();

        collectEqualsMatches(features, compiled, matchedCondIdsByLabelByGroup);
        collectContainsMatches(features, compiled, matchedCondIdsByLabelByGroup);

        Set<UUID> candidateLabelIds = new HashSet<>(matchedCondIdsByLabelByGroup.keySet());
        candidateLabelIds.addAll(compiled.notContainsLabelIds());
        if (candidateLabelIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Label> labelById = new HashMap<>();
        for (Label label : allLabels) {
            labelById.put(label.getId(), label);
        }

        List<Label> matched = new ArrayList<>();
        for (UUID candidateLabelId : candidateLabelIds) {
            Label label = labelById.get(candidateLabelId);
            if (label == null) {
                continue;
            }
            List<ParsedGroup> parsedGroups = compiled.parsedGroupsByLabel().get(candidateLabelId);
            if (parsedGroups == null) {
                continue;
            }

            if (evaluateDnf(features, parsedGroups)) {
                matched.add(label);
            }
        }

        return matched;
    }

    private void collectEqualsMatches(
            MessageFeatures features,
            CompiledRules compiled,
            Map<UUID, Map<String, Set<String>>> result
    ) {
        Map<String, List<ConditionRef>> equalsIndex = compiled.equalsIndex();

        checkEqualsField("MAIL_ACCOUNT", features.mailAccountId(), equalsIndex, result);
        checkEqualsField("FROM_DOMAIN", features.fromDomain(), equalsIndex, result);
        checkEqualsField("FROM_ADDRESS", features.fromAddress(), equalsIndex, result);

        if (features.hasAttachment()) {
            collectFromIndex("HAS_ATTACHMENT:true", equalsIndex, result);
        } else {
            collectFromIndex("HAS_ATTACHMENT:false", equalsIndex, result);
        }

        for (String toAddress : features.toAddresses()) {
            checkEqualsField("TO_ADDRESS", toAddress, equalsIndex, result);
        }
        for (String ccAddress : features.ccAddresses()) {
            checkEqualsField("CC_ADDRESS", ccAddress, equalsIndex, result);
        }
    }

    private void checkEqualsField(
            String field,
            String value,
            Map<String, List<ConditionRef>> equalsIndex,
            Map<UUID, Map<String, Set<String>>> result
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        collectFromIndex(field + ":" + value, equalsIndex, result);
    }

    private void collectFromIndex(
            String indexKey,
            Map<String, List<ConditionRef>> index,
            Map<UUID, Map<String, Set<String>>> result
    ) {
        List<ConditionRef> refs = index.get(indexKey);
        if (refs == null) {
            return;
        }
        for (ConditionRef ref : refs) {
            result.computeIfAbsent(ref.labelId(), k -> new HashMap<>())
                    .computeIfAbsent(ref.groupId(), k -> new HashSet<>())
                    .add(ref.conditionId());
        }
    }

    private void collectContainsMatches(
            MessageFeatures features,
            CompiledRules compiled,
            Map<UUID, Map<String, Set<String>>> result
    ) {
        Map<String, List<ConditionRef>> containsIndex = compiled.containsIndex();

        scanFieldWithAutomaton("SUBJECT", features.subject(), compiled.subjectAutomaton(), containsIndex, result);
        scanFieldWithAutomaton("BODY_TEXT", features.bodyText(), compiled.bodyTextAutomaton(), containsIndex, result);
        scanFieldWithAutomaton("FROM_ADDRESS", features.fromAddress(), compiled.fromAddressAutomaton(), containsIndex, result);
        scanFieldWithAutomaton("FROM_DOMAIN", features.fromDomain(), compiled.fromDomainAutomaton(), containsIndex, result);

        for (String toAddress : features.toAddresses()) {
            scanFieldWithAutomaton("TO_ADDRESS", toAddress, compiled.toAddressAutomaton(), containsIndex, result);
        }
        for (String ccAddress : features.ccAddresses()) {
            scanFieldWithAutomaton("CC_ADDRESS", ccAddress, compiled.ccAddressAutomaton(), containsIndex, result);
        }
    }

    private void scanFieldWithAutomaton(
            String field,
            String text,
            AhoCorasickAutomaton automaton,
            Map<String, List<ConditionRef>> containsIndex,
            Map<UUID, Map<String, Set<String>>> result
    ) {
        if (text == null || text.isBlank() || !automaton.hasPatterns()) {
            return;
        }

        Set<String> matchedKeywords = automaton.search(text);
        for (String keyword : matchedKeywords) {
            List<ConditionRef> refs = containsIndex.get(keyword);
            if (refs == null) {
                continue;
            }
            for (ConditionRef ref : refs) {
                if (!field.equals(ref.field())) {
                    continue;
                }
                result.computeIfAbsent(ref.labelId(), k -> new HashMap<>())
                        .computeIfAbsent(ref.groupId(), k -> new HashSet<>())
                        .add(ref.conditionId());
            }
        }
    }

    private boolean evaluateDnf(MessageFeatures features, List<ParsedGroup> groups) {
        for (ParsedGroup group : groups) {
            if (evaluateGroup(features, group)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateGroup(MessageFeatures features, ParsedGroup group) {
        for (ParsedCondition condition : group.conditions()) {
            if (!evaluateCondition(features, condition)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateCondition(MessageFeatures features, ParsedCondition condition) {
        String field = condition.field();
        String operator = condition.operator();
        String value = condition.normalizedValue();

        return switch (field) {
            case "MAIL_ACCOUNT"   -> evaluateString(features.mailAccountId(), operator, value);
            case "FROM_ADDRESS"   -> evaluateString(features.fromAddress(), operator, value);
            case "FROM_DOMAIN"    -> evaluateString(features.fromDomain(), operator, value);
            case "SUBJECT"        -> evaluateString(features.subject(), operator, value);
            case "BODY_TEXT"      -> evaluateString(features.bodyText(), operator, value);
            case "HAS_ATTACHMENT" -> evaluateBoolean(features.hasAttachment(), value);
            case "TO_ADDRESS"     -> "NOT_CONTAINS".equals(operator)
                    ? features.toAddresses().stream().allMatch(addr -> evaluateString(addr, operator, value))
                    : features.toAddresses().stream().anyMatch(addr -> evaluateString(addr, operator, value));
            case "CC_ADDRESS"     -> "NOT_CONTAINS".equals(operator)
                    ? features.ccAddresses().stream().allMatch(addr -> evaluateString(addr, operator, value))
                    : features.ccAddresses().stream().anyMatch(addr -> evaluateString(addr, operator, value));
            default               -> false;
        };
    }

    private boolean evaluateString(String target, String operator, String value) {
        if (target == null) {
            return "NOT_CONTAINS".equals(operator);
        }
        String normalizedTarget = target.trim().toLowerCase();
        return switch (operator) {
            case "EQUALS"       -> normalizedTarget.equals(value);
            case "CONTAINS"     -> normalizedTarget.contains(value);
            case "NOT_CONTAINS" -> !normalizedTarget.contains(value);
            default             -> false;
        };
    }

    private boolean evaluateBoolean(boolean target, String value) {
        return target == Boolean.parseBoolean(value);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Feature extraction
    // ─────────────────────────────────────────────────────────────────────────

    private MessageFeatures extractFeatures(Message message, boolean hasAttachment) {
        String fromAddress = normalize(message.getFromAddress());
        String fromDomain = extractDomain(fromAddress);
        String subject = normalize(message.getSubject());
        String bodyText = normalize(message.getBodyText());
        String mailAccountId = message.getThread() != null && message.getThread().getMailAccount() != null
                ? normalize(message.getThread().getMailAccount().getEmailAddress())
                : null;

        List<String> toAddresses = normalizeList(message.getToAddresses());
        List<String> ccAddresses = normalizeList(message.getCcAddresses());

        return new MessageFeatures(mailAccountId, fromAddress, fromDomain, subject, bodyText, hasAttachment, toAddresses, ccAddresses);
    }

    private String normalize(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase();
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) return null;
        return email.substring(email.indexOf('@') + 1);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(this::normalize)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule parsing
    // ─────────────────────────────────────────────────────────────────────────

    private List<ParsedGroup> parseGroups(Label label) {
        LabelRule rule = label.getRule();
        if (rule.groups() == null) {
            return List.of();
        }

        List<ParsedGroup> result = new ArrayList<>();
        for (LabelRule.Group group : rule.groups()) {
            if (group == null || group.conditions() == null) {
                continue;
            }

            String groupId = "g" + result.size();

            List<ParsedCondition> conditions = new ArrayList<>();
            int condIdx = 0;
            for (LabelRule.Condition condition : group.conditions()) {
                if (condition == null || condition.field() == null || condition.operator() == null) {
                    continue;
                }

                String field = condition.field().name();
                String operator = condition.operator().name();
                String normalizedValue = normalize(condition.value());
                String conditionId = groupId + "_c" + condIdx++;

                if (normalizedValue != null && normalizedValue.length() > MAX_VALUE_LENGTH) {
                    log.warn("Label condition value too long, skipping. labelId={} field={}", label.getId(), field);
                    continue;
                }

                conditions.add(new ParsedCondition(conditionId, field, operator, normalizedValue));
            }
            if (!conditions.isEmpty()) {
                result.add(new ParsedGroup(groupId, conditions));
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal value types
    // ─────────────────────────────────────────────────────────────────────────

    record ConditionRef(UUID labelId, String groupId, String conditionId, String field, String operator, String normalizedValue) {}

    record ParsedCondition(String conditionId, String field, String operator, String normalizedValue) {}

    record ParsedGroup(String groupId, List<ParsedCondition> conditions) {}

    record MessageFeatures(
            String mailAccountId,
            String fromAddress,
            String fromDomain,
            String subject,
            String bodyText,
            boolean hasAttachment,
            List<String> toAddresses,
            List<String> ccAddresses
    ) {}

    record CompiledRules(
            Map<String, List<ConditionRef>> equalsIndex,
            Map<String, List<ConditionRef>> containsIndex,
            List<ConditionRef> notContainsRefs,
            Set<UUID> notContainsLabelIds,
            Map<UUID, List<ParsedGroup>> parsedGroupsByLabel,
            AhoCorasickAutomaton subjectAutomaton,
            AhoCorasickAutomaton bodyTextAutomaton,
            AhoCorasickAutomaton fromAddressAutomaton,
            AhoCorasickAutomaton fromDomainAutomaton,
            AhoCorasickAutomaton toAddressAutomaton,
            AhoCorasickAutomaton ccAddressAutomaton
    ) {}
}
