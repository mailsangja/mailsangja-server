package com.mailsangja.worker.dto.mail.sync;

import java.util.List;

public record InitialMailSyncThreadSaveCommand(
        String gmailThreadId,
        String historyId,
        List<InitialMailSyncMessageSaveCommand> messages
) {
    public static InitialMailSyncThreadSaveCommand from(InitialMailSyncThreadResult result) {
        return new InitialMailSyncThreadSaveCommand(
                result.gmailThreadId(),
                result.historyId(),
                result.messages().stream()
                        .map(InitialMailSyncMessageSaveCommand::from)
                        .toList()
        );
    }
}
