package com.mailsangja.db.dto;

import java.util.UUID;

public interface ThreadLabelProjection {
    UUID getThreadId();
    UUID getLabelId();
    String getLabelName();
    String getLabelColorCode();
}
