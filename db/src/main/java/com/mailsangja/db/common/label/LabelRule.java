package com.mailsangja.db.common.label;

import java.util.List;

public record LabelRule(List<Group> groups) {

    public record Group(List<Condition> conditions) {}

    public record Condition(Field field, Operator operator, String value) {}

    public enum Field {
        MAIL_ACCOUNT,
        FROM_ADDRESS,
        FROM_DOMAIN,
        TO_ADDRESS,
        CC_ADDRESS,
        SUBJECT,
        BODY_TEXT,
        HAS_ATTACHMENT
    }

    public enum Operator {
        EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        BOOLEAN
    }
}
