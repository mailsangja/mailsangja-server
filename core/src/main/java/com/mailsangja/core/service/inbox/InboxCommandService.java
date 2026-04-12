package com.mailsangja.core.service.inbox;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InboxCommandService {

    private final MessageRepositoryPort messageRepositoryPort;

    @Transactional
    public void markThreadAsRead(Thread thread) {
        thread.updateReadStatus(true);

        List<Message> messages = messageRepositoryPort.findAllByThreadIdAndDeletedAtIsNull(thread.getId());
        messages.forEach(Message::markAsRead);
    }
}
