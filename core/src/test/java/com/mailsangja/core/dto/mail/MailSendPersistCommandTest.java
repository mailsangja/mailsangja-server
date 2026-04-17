package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailSendPersistCommandTest {

    @Test
    void toCreateValues_전송입력의이름을우선해서저장한다() {
        MailSendPersistCommand command = new MailSendPersistCommand(
                MailAccount.builder().id(UUID.randomUUID()).build(),
                new GoogleMailMessageResult(
                        "gmail-message-id",
                        "gmail-thread-id",
                        "history-id",
                        "제목",
                        "sender@example.com",
                        null,
                        List.of("to@example.com"),
                        List.of(),
                        List.of("cc@example.com"),
                        List.of(),
                        "snippet",
                        LocalDateTime.of(2026, 4, 16, 18, 0),
                        "본문",
                        null,
                        List.of()
                ),
                new MailSendCommand(
                        UUID.randomUUID(),
                        new MailAddressCommand("보내는사람", "sender@example.com"),
                        List.of(new MailAddressCommand("받는사람", "to@example.com")),
                        List.of(new MailAddressCommand("참조사람", "cc@example.com")),
                        List.of(),
                        "제목",
                        "본문",
                        List.of()
                )
        );

        Message.CreateValues values = command.toCreateValues();

        assertEquals(Direction.OUTBOUND, values.direction());
        assertEquals("보내는사람", values.fromName());
        assertEquals(List.of("받는사람"), values.toNames());
        assertEquals(List.of("참조사람"), values.ccNames());
    }

    @Test
    void fromRaw_이름이없으면이메일주소를이름으로채운다() {
        MailAddressCommand command = MailAddressCommand.fromRaw("user@example.com");

        assertEquals("user@example.com", command.address());
        assertEquals("user@example.com", command.name());
    }

    @Test
    void latestParticipantAddress_to가없으면cc를대표참여자로사용한다() {
        MailSendPersistCommand command = new MailSendPersistCommand(
                MailAccount.builder().id(UUID.randomUUID()).build(),
                new GoogleMailMessageResult(
                        "gmail-message-id",
                        "gmail-thread-id",
                        "history-id",
                        "제목",
                        "sender@example.com",
                        "sender@example.com",
                        List.of(),
                        List.of(),
                        List.of("cc@example.com"),
                        List.of("참조사람"),
                        "snippet",
                        LocalDateTime.of(2026, 4, 16, 18, 0),
                        "본문",
                        null,
                        List.of()
                ),
                new MailSendCommand(
                        UUID.randomUUID(),
                        new MailAddressCommand("sender@example.com", "sender@example.com"),
                        List.of(),
                        List.of(new MailAddressCommand("참조사람", "cc@example.com")),
                        List.of(),
                        "제목",
                        "본문",
                        List.of()
                )
        );

        assertEquals("cc@example.com", command.latestParticipantAddress());
    }

    @Test
    void latestParticipantName_to가없으면cc의이름을대표참여자로사용한다() {
        MailSendPersistCommand command = new MailSendPersistCommand(
                MailAccount.builder().id(UUID.randomUUID()).build(),
                new GoogleMailMessageResult(
                        "gmail-message-id",
                        "gmail-thread-id",
                        "history-id",
                        "제목",
                        "sender@example.com",
                        "sender@example.com",
                        List.of(),
                        List.of(),
                        List.of("cc@example.com"),
                        List.of("참조사람"),
                        "snippet",
                        LocalDateTime.of(2026, 4, 16, 18, 0),
                        "본문",
                        null,
                        List.of()
                ),
                new MailSendCommand(
                        UUID.randomUUID(),
                        new MailAddressCommand("sender@example.com", "sender@example.com"),
                        List.of(),
                        List.of(new MailAddressCommand("참조사람", "cc@example.com")),
                        List.of(),
                        "제목",
                        "본문",
                        List.of()
                )
        );

        assertEquals("참조사람", command.latestParticipantName());
    }
}
