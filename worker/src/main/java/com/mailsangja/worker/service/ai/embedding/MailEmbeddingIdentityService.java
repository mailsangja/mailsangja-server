package com.mailsangja.worker.service.ai.embedding;

import com.mailsangja.db.entity.mail.Message;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MailEmbeddingIdentityService {

    public UUID createDocumentId(Message message) {
        return null;
    }
}
