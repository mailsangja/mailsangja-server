package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.dto.ThreadMessageLabelView;

import java.util.List;
import java.util.Map;

public record ThreadDetailResult(
        Thread thread,
        List<Message> messages,
        Map<String, String> contactNameByEmail,
        List<ThreadMessageLabelView> labels
) {}
