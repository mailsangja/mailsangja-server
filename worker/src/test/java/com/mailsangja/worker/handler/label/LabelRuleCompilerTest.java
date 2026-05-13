package com.mailsangja.worker.handler.label;

import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.worker.dto.label.MessageBatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.mailsangja.db.common.label.LabelRule.Field.BODY_TEXT;
import static com.mailsangja.db.common.label.LabelRule.Field.CC_ADDRESS;
import static com.mailsangja.db.common.label.LabelRule.Field.FROM_ADDRESS;
import static com.mailsangja.db.common.label.LabelRule.Field.FROM_DOMAIN;
import static com.mailsangja.db.common.label.LabelRule.Field.HAS_ATTACHMENT;
import static com.mailsangja.db.common.label.LabelRule.Field.MAIL_ACCOUNT;
import static com.mailsangja.db.common.label.LabelRule.Field.SUBJECT;
import static com.mailsangja.db.common.label.LabelRule.Field.TO_ADDRESS;
import static com.mailsangja.db.common.label.LabelRule.Operator.BOOLEAN;
import static com.mailsangja.db.common.label.LabelRule.Operator.CONTAINS;
import static com.mailsangja.db.common.label.LabelRule.Operator.EQUALS;
import static com.mailsangja.db.common.label.LabelRule.Operator.NOT_CONTAINS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelRuleCompilerTest {

    private final LabelRuleCompiler compiler = new LabelRuleCompiler();

    @Test
    void compile_emptyLabelsOrMessages_returnsEmptyMap() {
        Message message = message("Invoice", "finance@example.com", "body", List.of(), List.of(), "user@example.com");

        assertTrue(compiler.compile(List.of(), new MessageBatch(List.of(message), Set.of())).isEmpty());
        assertTrue(compiler.compile(List.of(label("업무", rule(condition(SUBJECT, CONTAINS, "invoice")))),
                new MessageBatch(List.of(), Set.of())).isEmpty());
    }

    @Test
    void compile_matchesEqualsAndContainsAndBooleanConditions() {
        Message message = message(
                "Quarterly Invoice",
                "Finance@Example.com",
                "Please check attached file",
                List.of("USER@EXAMPLE.COM"),
                List.of("manager@example.com"),
                "user@example.com"
        );
        Label label = label("복합 조건", rule(
                condition(MAIL_ACCOUNT, EQUALS, "USER@example.com"),
                condition(FROM_DOMAIN, EQUALS, "example.com"),
                condition(FROM_ADDRESS, CONTAINS, "finance"),
                condition(TO_ADDRESS, EQUALS, "user@example.com"),
                condition(CC_ADDRESS, CONTAINS, "manager"),
                condition(SUBJECT, CONTAINS, "invoice"),
                condition(BODY_TEXT, CONTAINS, "attached"),
                condition(HAS_ATTACHMENT, BOOLEAN, "true")
        ));

        Map<UUID, List<Label>> result = compiler.compile(
                List.of(label),
                new MessageBatch(List.of(message), Set.of(message.getId()))
        );

        assertEquals(List.of(label), result.get(message.getId()));
    }

    @Test
    void compile_groupConditionsUseAndWithinGroup() {
        Message message = message("Invoice", "finance@example.com", "body", List.of(), List.of(), "user@example.com");
        Label label = label("AND 조건", rule(
                condition(SUBJECT, CONTAINS, "invoice"),
                condition(BODY_TEXT, CONTAINS, "missing")
        ));

        Map<UUID, List<Label>> result = compiler.compile(List.of(label), new MessageBatch(List.of(message), Set.of()));

        assertTrue(result.get(message.getId()).isEmpty());
    }

    @Test
    void compile_groupsUseOrBetweenGroups() {
        Message message = message("Invoice", "finance@example.com", "body", List.of(), List.of(), "user@example.com");
        Label label = label("OR 조건", new LabelRule(List.of(
                new LabelRule.Group(List.of(condition(SUBJECT, CONTAINS, "missing"))),
                new LabelRule.Group(List.of(condition(FROM_DOMAIN, EQUALS, "example.com")))
        )));

        Map<UUID, List<Label>> result = compiler.compile(List.of(label), new MessageBatch(List.of(message), Set.of()));

        assertEquals(List.of(label), result.get(message.getId()));
    }

    @Test
    void compile_notContainsMatchesWhenTextDoesNotContainValue() {
        Message message = message("Status report", "sender@example.com", "no urgent content", List.of(), List.of(), "user@example.com");
        Label label = label("광고 아님", rule(condition(SUBJECT, NOT_CONTAINS, "ad")));

        Map<UUID, List<Label>> result = compiler.compile(List.of(label), new MessageBatch(List.of(message), Set.of()));

        assertEquals(List.of(label), result.get(message.getId()));
    }

    @Test
    void compile_notContainsForRecipientsRequiresAllRecipientsNotContainingValue() {
        Message matched = message("subject", "sender@example.com", "body",
                List.of("first@example.com", "second@example.com"), List.of("safe@example.com"), "user@example.com");
        Message unmatched = message("subject", "sender@example.com", "body",
                List.of("vip@example.com", "second@example.com"), List.of(), "user@example.com");
        Label label = label("VIP 제외", rule(condition(TO_ADDRESS, NOT_CONTAINS, "vip")));

        Map<UUID, List<Label>> result = compiler.compile(List.of(label), new MessageBatch(List.of(matched, unmatched), Set.of()));

        assertEquals(List.of(label), result.get(matched.getId()));
        assertTrue(result.get(unmatched.getId()).isEmpty());
    }

    @Test
    void compile_labelWithoutRuleOrInvalidOverlongGroupIsIgnored() {
        Message message = message("Invoice", "finance@example.com", "body", List.of(), List.of(), "user@example.com");
        Label noRule = label("규칙 없음", null);
        Label overlong = label("긴 값", rule(condition(SUBJECT, CONTAINS, "a".repeat(201))));

        Map<UUID, List<Label>> result = compiler.compile(List.of(noRule, overlong), new MessageBatch(List.of(message), Set.of()));

        assertTrue(result.get(message.getId()).isEmpty());
    }

    private Label label(String name, LabelRule rule) {
        return Label.builder()
                .id(UUID.randomUUID())
                .name(name)
                .rule(rule)
                .build();
    }

    private LabelRule rule(LabelRule.Condition... conditions) {
        return new LabelRule(List.of(new LabelRule.Group(List.of(conditions))));
    }

    private LabelRule.Condition condition(LabelRule.Field field, LabelRule.Operator operator, String value) {
        return new LabelRule.Condition(field, operator, value);
    }

    private Message message(
            String subject,
            String fromAddress,
            String bodyText,
            List<String> toAddresses,
            List<String> ccAddresses,
            String mailAccountEmail
    ) {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .emailAddress(mailAccountEmail)
                .build();
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(UUID.randomUUID().toString())
                .direction(Direction.INBOUND)
                .build();
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(UUID.randomUUID().toString())
                .direction(Direction.INBOUND)
                .subject(subject)
                .fromAddress(fromAddress)
                .toAddresses(toAddresses)
                .ccAddresses(ccAddresses)
                .bodyText(bodyText)
                .read(false)
                .build();
    }
}
