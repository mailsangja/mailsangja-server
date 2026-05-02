package com.mailsangja.worker.dto.label;

import com.mailsangja.db.entity.mail.Message;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MessageBatch(
        List<Message> messages,
        Set<UUID> messageIdsWithAttachments
) {}
