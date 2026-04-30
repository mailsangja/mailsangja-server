package com.mailsangja.db.module.mail;

import java.util.UUID;

public interface ThreadLabelProjection {
    UUID getThreadId();
    UUID getLabelId();
    String getLabelName();
    String getLabelColorCode();
}
