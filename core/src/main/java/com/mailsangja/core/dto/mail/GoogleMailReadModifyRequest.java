package com.mailsangja.core.dto.mail;

import java.util.List;

public record GoogleMailReadModifyRequest(
        List<String> removeLabelIds
) {
}
