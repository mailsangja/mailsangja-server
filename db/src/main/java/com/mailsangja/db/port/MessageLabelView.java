package com.mailsangja.db.port;

import java.util.UUID;

public record MessageLabelView(
        UUID messageId,
        UUID labelId,
        String labelName,
        String colorCode
) {}
