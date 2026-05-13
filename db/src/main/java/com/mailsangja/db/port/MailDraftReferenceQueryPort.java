package com.mailsangja.db.port;

import java.util.List;
import java.util.UUID;

public interface MailDraftReferenceQueryPort {

    List findRecentWrittenMessages(UUID userId, UUID mailAccountId, int limit);

    List searchOwnWrittenMessages(UUID userId, UUID mailAccountId, String query, int limit);

    List searchOtherRelevantMessages(UUID userId, UUID mailAccountId, String query, int limit);

    List findThreadContextMessages(UUID replyMessageId);
}
