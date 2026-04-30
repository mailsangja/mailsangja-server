package com.mailsangja.core.common.util;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.db.common.label.LabelRule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class LabelRuleValidator {

    private static final int MAX_VALUE_LENGTH = 200;

    private static final Map<LabelRule.Field, Set<LabelRule.Operator>> FIELD_OPERATOR_MAP = Map.of(
            LabelRule.Field.MAIL_ACCOUNT,   Set.of(LabelRule.Operator.EQUALS),
            LabelRule.Field.FROM_ADDRESS,   Set.of(LabelRule.Operator.EQUALS, LabelRule.Operator.CONTAINS, LabelRule.Operator.NOT_CONTAINS),
            LabelRule.Field.FROM_DOMAIN,    Set.of(LabelRule.Operator.EQUALS, LabelRule.Operator.CONTAINS, LabelRule.Operator.NOT_CONTAINS),
            LabelRule.Field.TO_ADDRESS,     Set.of(LabelRule.Operator.EQUALS, LabelRule.Operator.CONTAINS, LabelRule.Operator.NOT_CONTAINS),
            LabelRule.Field.CC_ADDRESS,     Set.of(LabelRule.Operator.EQUALS, LabelRule.Operator.CONTAINS, LabelRule.Operator.NOT_CONTAINS),
            LabelRule.Field.SUBJECT,        Set.of(LabelRule.Operator.CONTAINS, LabelRule.Operator.NOT_CONTAINS),
            LabelRule.Field.BODY_TEXT,      Set.of(LabelRule.Operator.CONTAINS, LabelRule.Operator.NOT_CONTAINS),
            LabelRule.Field.HAS_ATTACHMENT, Set.of(LabelRule.Operator.BOOLEAN)
    );

    public void validate(LabelRule rule) {
        if (rule == null) {
            return;
        }

        if (rule.groups() == null) {
            return;
        }

        for (LabelRule.Group group : rule.groups()) {
            if (group == null || group.conditions() == null) {
                continue;
            }
            for (LabelRule.Condition condition : group.conditions()) {
                validateCondition(condition);
            }
        }
    }

    private void validateCondition(LabelRule.Condition condition) {
        LabelRule.Field field = condition.field();
        LabelRule.Operator operator = condition.operator();
        String value = condition.value();

        if (field == null) {
            throw new LabelException(LabelErrorCode.LABEL_RULE_INVALID_FIELD);
        }

        if (operator == null) {
            throw new LabelException(LabelErrorCode.LABEL_RULE_INVALID_OPERATOR);
        }

        Set<LabelRule.Operator> allowedOperators = FIELD_OPERATOR_MAP.get(field);
        if (allowedOperators == null || !allowedOperators.contains(operator)) {
            throw new LabelException(LabelErrorCode.LABEL_RULE_FIELD_OPERATOR_MISMATCH);
        }

        if (operator == LabelRule.Operator.BOOLEAN) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new LabelException(LabelErrorCode.LABEL_RULE_INVALID_BOOLEAN_VALUE);
            }
        } else {
            if (value == null || value.isBlank()) {
                throw new LabelException(LabelErrorCode.LABEL_RULE_VALUE_BLANK);
            }
            if (value.length() > MAX_VALUE_LENGTH) {
                throw new LabelException(LabelErrorCode.LABEL_RULE_VALUE_TOO_LONG);
            }
        }
    }
}
