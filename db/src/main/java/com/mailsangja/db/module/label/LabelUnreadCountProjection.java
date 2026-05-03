package com.mailsangja.db.module.label;

import java.util.UUID;

public interface LabelUnreadCountProjection {
    UUID getLabelId();
    Long getUnreadCount();
}
