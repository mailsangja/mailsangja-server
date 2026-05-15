package com.mailsangja.core.common.util;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.db.common.label.LabelRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.mailsangja.db.common.label.LabelRule.Field.BODY_TEXT;
import static com.mailsangja.db.common.label.LabelRule.Field.FROM_ADDRESS;
import static com.mailsangja.db.common.label.LabelRule.Field.HAS_ATTACHMENT;
import static com.mailsangja.db.common.label.LabelRule.Field.MAIL_ACCOUNT;
import static com.mailsangja.db.common.label.LabelRule.Field.SUBJECT;
import static com.mailsangja.db.common.label.LabelRule.Operator.BOOLEAN;
import static com.mailsangja.db.common.label.LabelRule.Operator.CONTAINS;
import static com.mailsangja.db.common.label.LabelRule.Operator.EQUALS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LabelRuleValidatorTest {

    private final LabelRuleValidator validator = new LabelRuleValidator();

    @Test
    void validate_nullRule_isAllowed() {
        assertDoesNotThrow(() -> validator.validate(null));
    }

    @Test
    void validate_validRule_isAllowed() {
        LabelRule rule = rule(
                condition(FROM_ADDRESS, CONTAINS, "sender@example.com"),
                condition(HAS_ATTACHMENT, BOOLEAN, "true")
        );

        assertDoesNotThrow(() -> validator.validate(rule));
    }

    @Test
    void validate_groupsIsNull_throwsInvalidJson() {
        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(new LabelRule(null)));

        assertEquals(LabelErrorCode.LABEL_RULE_INVALID_JSON, exception.getErrorCode());
    }

    @Test
    void validate_groupIsNull_throwsInvalidJson() {
        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(new LabelRule(Arrays.asList((LabelRule.Group) null))));

        assertEquals(LabelErrorCode.LABEL_RULE_INVALID_JSON, exception.getErrorCode());
    }

    @Test
    void validate_conditionIsNull_throwsInvalidField() {
        LabelRule rule = new LabelRule(List.of(new LabelRule.Group(Arrays.asList((LabelRule.Condition) null))));

        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(rule));

        assertEquals(LabelErrorCode.LABEL_RULE_INVALID_FIELD, exception.getErrorCode());
    }

    @Test
    void validate_fieldIsNull_throwsInvalidField() {
        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(rule(condition(null, EQUALS, "value"))));

        assertEquals(LabelErrorCode.LABEL_RULE_INVALID_FIELD, exception.getErrorCode());
    }

    @Test
    void validate_operatorIsNull_throwsInvalidOperator() {
        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(rule(condition(FROM_ADDRESS, null, "value"))));

        assertEquals(LabelErrorCode.LABEL_RULE_INVALID_OPERATOR, exception.getErrorCode());
    }

    @Test
    void validate_fieldOperatorMismatch_throwsMismatch() {
        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(rule(condition(SUBJECT, EQUALS, "title"))));

        assertEquals(LabelErrorCode.LABEL_RULE_FIELD_OPERATOR_MISMATCH, exception.getErrorCode());
    }

    @Test
    void validate_booleanOperatorWithNonBooleanValue_throwsInvalidBooleanValue() {
        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(rule(condition(HAS_ATTACHMENT, BOOLEAN, "yes"))));

        assertEquals(LabelErrorCode.LABEL_RULE_INVALID_BOOLEAN_VALUE, exception.getErrorCode());
    }

    @Test
    void validate_nonBooleanOperatorWithBlankValue_throwsValueBlank() {
        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(rule(condition(BODY_TEXT, CONTAINS, "  "))));

        assertEquals(LabelErrorCode.LABEL_RULE_VALUE_BLANK, exception.getErrorCode());
    }

    @Test
    void validate_nonBooleanOperatorWithTooLongValue_throwsValueTooLong() {
        LabelException exception = assertThrows(LabelException.class, () -> validator.validate(rule(condition(MAIL_ACCOUNT, EQUALS, "a".repeat(201)))));

        assertEquals(LabelErrorCode.LABEL_RULE_VALUE_TOO_LONG, exception.getErrorCode());
    }

    private LabelRule rule(LabelRule.Condition... conditions) {
        return new LabelRule(List.of(new LabelRule.Group(List.of(conditions))));
    }

    private LabelRule.Condition condition(
            LabelRule.Field field,
            LabelRule.Operator operator,
            String value
    ) {
        return new LabelRule.Condition(field, operator, value);
    }
}
