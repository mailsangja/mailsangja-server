package com.mailsangja.db.dto;

import java.util.UUID;

public interface MessageLabelProjection {
    UUID getMessageId();
    UUID getLabelId();
    String getLabelName();
    String getColorCode();
}
