package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.dto.ThreadMessageLabelView;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ThreadListResult(
        Slice<Thread> threads,
        Map<UUID, List<Attachment>> attachmentsByThreadId,
        Map<String, String> contactNameByEmail,
        Map<UUID, List<ThreadMessageLabelView>> labelsByThreadId
) {}
