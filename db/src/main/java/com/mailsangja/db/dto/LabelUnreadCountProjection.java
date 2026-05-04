package com.mailsangja.db.dto;

import java.util.UUID;

public interface LabelUnreadCountProjection {
    UUID getLabelId();
    Long getUnreadCount();
}
