package com.mailsangja.db.dto;

import java.util.UUID;

public record ThreadLabelView(
        UUID threadId,
        UUID labelId,
        String labelName,
        String colorCode
) {}
