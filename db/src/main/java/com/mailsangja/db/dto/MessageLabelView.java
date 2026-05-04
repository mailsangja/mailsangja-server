package com.mailsangja.db.dto;

import java.util.UUID;

public record MessageLabelView(
        UUID messageId,
        UUID labelId,
        String labelName,
        String colorCode
) {}
