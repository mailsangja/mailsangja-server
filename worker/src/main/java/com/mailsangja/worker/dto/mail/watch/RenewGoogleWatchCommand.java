package com.mailsangja.worker.dto.mail.watch;

import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;

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
