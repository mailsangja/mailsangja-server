package com.mailsangja.db.dto;

import java.util.UUID;

public interface ThreadMessageLabelProjection {
    UUID getThreadId();
    UUID getLabelId();
    String getLabelName();
    String getLabelColorCode();
    boolean getLabelIsSensitive();
}
