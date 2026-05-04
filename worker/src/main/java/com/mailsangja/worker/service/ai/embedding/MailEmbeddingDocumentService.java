package com.mailsangja.worker.service.ai.embedding;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class MailEmbeddingDocumentService {

    public boolean hasBodyText(Message message) {
        return message != null && !isBlank(message.getBodyText());
    }

    public Document build(Message message) {
        return Document.builder()
                .id(message.getId().toString())
                .text(message.getBodyText())
                .metadata(buildMetadata(message))
                .build();
    }

    public Document build(Message message, UUID documentId, String maskedText) {
        return null;
    }

    private Map<String, Object> buildMetadata(Message message) {
        Thread thread = message.getThread();
        MailAccount mailAccount = thread.getMailAccount();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("UserId", mailAccount.getUser().getId().toString());
        metadata.put("MailAccountId", mailAccount.getId().toString());
        metadata.put("MessageId", message.getId().toString());
        metadata.put("ThreadId", thread.getId().toString());
        addAddressMetadata(metadata, message);
        return metadata;
    }

    private void addAddressMetadata(Map<String, Object> metadata, Message message) {
        metadata.put("ReceivedAt", receivedAt(message));
        metadata.put("FromMailAddress", message.getFromAddress());
        metadata.put("ToMailAddress", toMailAddresses(message));
    }

    private String receivedAt(Message message) {
        if (message.getSentAt() == null) {
            return null;
        }
        return message.getSentAt().toString();
    }

    private List<String> toMailAddresses(Message message) {
        if (message.getToAddresses() == null) {
            return List.of();
        }
        return message.getToAddresses();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
