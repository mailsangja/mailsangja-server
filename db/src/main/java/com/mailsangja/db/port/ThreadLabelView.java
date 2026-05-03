package com.mailsangja.db.port;

import java.util.UUID;

public record ThreadLabelView(
        UUID threadId,
        UUID labelId,
        String labelName,
        String colorCode
) {}
