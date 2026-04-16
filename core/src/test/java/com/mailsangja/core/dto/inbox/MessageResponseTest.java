package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageResponseTest {

    @Test
    void from_저장된GmailName을우선하고없으면연락처이름을사용한다() {
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .gmailMessageId("gmail-message-id")
                .direction(Direction.INBOUND)
                .subject("subject")
                .fromAddress("sender@example.com")
                .fromName("Gmail Sender")
                .toAddresses(List.of("first@example.com", "second@example.com"))
                .toNames(Arrays.asList("First Gmail Name", null))
                .ccAddresses(List.of("cc@example.com"))
                .ccNames(Arrays.asList((String) null))
                .snippet("snippet")
                .read(true)
                .build();

        MessageResponse response = MessageResponse.from(
                message,
                Map.of(
                        "sender@example.com", "Contact Sender",
                        "first@example.com", "Contact First",
                        "second@example.com", "Contact Second",
                        "cc@example.com", "Contact Cc"
                )
        );

        assertEquals("Gmail Sender", response.from().name());
        assertEquals("First Gmail Name", response.to().getFirst().name());
        assertEquals("Contact Second", response.to().get(1).name());
        assertEquals("Contact Cc", response.cc().getFirst().name());
    }
}
