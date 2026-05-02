package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageLabelView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ThreadDetailResult(
        Thread thread,
        List<Message> messages,
        Map<String, String> contactNameByEmail,
        Map<UUID, List<MessageLabelView>> messageLabelsByMessageId
) {}
