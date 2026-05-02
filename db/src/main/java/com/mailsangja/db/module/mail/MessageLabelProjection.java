package com.mailsangja.db.module.mail;

import java.util.UUID;

public interface MessageLabelProjection {
    UUID getMessageId();
    UUID getLabelId();
    String getLabelName();
    String getColorCode();
}
