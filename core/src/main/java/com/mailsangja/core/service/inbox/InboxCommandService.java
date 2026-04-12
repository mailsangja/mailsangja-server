package com.mailsangja.core.service.inbox;

import com.mailsangja.db.entity.mail.Thread;
import org.springframework.stereotype.Service;

@Service
public class InboxCommandService {

    public void markThreadAsRead(Thread thread) {
        if (thread == null) {
            return;
        }
    }
}
