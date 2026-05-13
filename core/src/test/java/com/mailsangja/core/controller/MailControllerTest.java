package com.mailsangja.core.controller;

import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.dto.mail.MailDraftStreamRequest;
import com.mailsangja.core.facade.MailFacade;
import com.mailsangja.core.facade.MailDraftFacade;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailControllerTest {

    @Test
    void sendMail_messageId가없으면기존sendMail을호출한다() {
        // given
        MailFacade mailFacade = mock(MailFacade.class);
        MailDraftFacade mailDraftFacade = mock(MailDraftFacade.class);
        MailController controller = new MailController(mailFacade, mailDraftFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        MailSendRequest request = new MailSendRequest(
                "\"Sender\" <sender@example.com>",
                null,
                List.of("\"To\" <to@example.com>"),
                null,
                null,
                "제목",
                "본문",
                null
        );

        // when
        controller.sendMail(user, null, request);

        // then
        verify(mailFacade).sendMail(user, request);
        verify(mailFacade, never()).replyMail(any(), any(), any());
    }

    @Test
    void sendMail_messageId가있으면replyMail을호출한다() {
        // given
        MailFacade mailFacade = mock(MailFacade.class);
        MailDraftFacade mailDraftFacade = mock(MailDraftFacade.class);
        MailController controller = new MailController(mailFacade, mailDraftFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        UUID messageId = UUID.randomUUID();
        MailSendRequest request = new MailSendRequest(
                "\"Sender\" <sender@example.com>",
                null,
                List.of("\"To\" <to@example.com>"),
                null,
                null,
                "Re: 제목",
                "본문",
                null
        );

        // when
        controller.sendMail(user, messageId, request);

        // then
        verify(mailFacade).replyMail(user, messageId, request);
        verify(mailFacade, never()).sendMail(any(), any());
    }

    @Test
    void streamDraft_ResponseEntitySseEmitter를반환한다() {
        // given
        MailFacade mailFacade = mock(MailFacade.class);
        MailDraftFacade mailDraftFacade = mock(MailDraftFacade.class);
        MailController controller = new MailController(mailFacade, mailDraftFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        MailDraftStreamRequest request = new MailDraftStreamRequest(
                UUID.randomUUID(),
                "거래처에 일정 조율 메일 초안 작성",
                null,
                List.of("to@example.com"),
                List.of("cc@example.com")
        );
        SseEmitter emitter = new SseEmitter();
        when(mailDraftFacade.streamDraft(user, request)).thenReturn(emitter);

        // when
        ResponseEntity<SseEmitter> response = controller.streamDraft(user, request);

        // then
        assertEquals(200, response.getStatusCode().value());
        assertSame(emitter, response.getBody());
        verify(mailDraftFacade).streamDraft(user, request);
        verifyNoMailSend(mailFacade);
    }

    private void verifyNoMailSend(MailFacade mailFacade) {
        verify(mailFacade, never()).sendMail(any(), any());
        verify(mailFacade, never()).replyMail(any(), any(), any());
    }
}
