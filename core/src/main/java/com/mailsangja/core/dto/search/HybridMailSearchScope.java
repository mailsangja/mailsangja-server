package com.mailsangja.core.dto.search;

import com.mailsangja.db.entity.mail.Direction;

public enum HybridMailSearchScope {
    ALL,
    INBOX,
    SENT;

    public Direction direction() {
        return switch (this) {
            case ALL -> null;
            case INBOX -> Direction.INBOUND;
            case SENT -> Direction.OUTBOUND;
        };
    }
}
