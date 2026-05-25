package com.mailsangja.db.dto;

import java.util.UUID;

public record ThreadMessageLabelView(
        UUID threadId,
        UUID labelId,
        String labelName,
        String colorCode,
        boolean isSensitive
) {}
