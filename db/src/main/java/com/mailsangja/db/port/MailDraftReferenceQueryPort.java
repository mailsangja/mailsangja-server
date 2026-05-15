package com.mailsangja.db.port;

import com.mailsangja.db.dto.MailDraftReferenceMessageResult;

import java.util.List;
import java.util.UUID;

public interface MailDraftReferenceQueryPort {

    List<MailDraftReferenceMessageResult> findRecentWrittenMessages(UUID userId, UUID mailAccountId, int limit);

    List<MailDraftReferenceMessageResult> findWrittenMessagesByHints(UUID userId, UUID mailAccountId, List<String> hints, int limit);

    List<MailDraftReferenceMessageResult> findMessagesByIds(List<UUID> messageIds);

    List<MailDraftReferenceMessageResult> findThreadContextMessages(UUID replyMessageId);
}
