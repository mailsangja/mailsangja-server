package com.mailsangja.core.dto.mail;

import java.util.List;

public record GoogleMailUnreadModifyRequest(
        List<String> addLabelIds
) {
}
