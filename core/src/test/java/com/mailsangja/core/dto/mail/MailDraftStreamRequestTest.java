package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailDraftStreamRequestTest {

    @Test
    void mailAccountId가없으면실패한다() {
        // given

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftStreamRequest(
                null,
                "일정 조율 메일 초안 작성",
                null,
                List.of("to@example.com"),
                null
        ));
    }

    @Test
    void query가blank이면실패한다() {
        // given

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftStreamRequest(
                UUID.randomUUID(),
                " ",
                null,
                List.of("to@example.com"),
                null
        ));
    }

    @Test
    void to가비어있으면실패한다() {
        // given

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftStreamRequest(
                UUID.randomUUID(),
                "일정 조율 메일 초안 작성",
                null,
                List.of(),
                null
        ));
    }

    @Test
    void to가비어있고cc만있어도실패한다() {
        // given

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftStreamRequest(
                UUID.randomUUID(),
                "일정 조율 메일 초안 작성",
                null,
                null,
                List.of("cc@example.com")
        ));
    }

    @Test
    void 필수값이있으면성공한다() {
        // given

        // when & then
        assertDoesNotThrow(() -> new MailDraftStreamRequest(
                UUID.randomUUID(),
                "거래처에 일정 조율 메일 초안 작성",
                null,
                List.of("to@example.com"),
                List.of("cc@example.com")
        ));
    }
}
