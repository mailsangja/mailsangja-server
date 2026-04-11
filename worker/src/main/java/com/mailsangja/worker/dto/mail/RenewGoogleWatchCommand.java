package com.mailsangja.worker.dto.mail;

import com.mailsangja.worker.dto.gmail.GoogleMailWatchResult;
import com.mailsangja.worker.dto.gmail.GoogleOAuthTokenResult;

import java.util.UUID;

public record RenewGoogleWatchCommand(
        UUID mailAccountId,
        GoogleOAuthTokenResult tokenResult,
        GoogleMailWatchResult watchResult
) {

    public static RenewGoogleWatchCommand of(
            UUID mailAccountId,
            GoogleOAuthTokenResult tokenResult,
            GoogleMailWatchResult watchResult
    ) {
        return new RenewGoogleWatchCommand(mailAccountId, tokenResult, watchResult);
    }
}
