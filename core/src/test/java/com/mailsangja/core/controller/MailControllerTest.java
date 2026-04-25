package com.mailsangja.core.controller;

import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.facade.MailFacade;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MailControllerTest {

    @Test
    void sendMail_messageId가없으면기존sendMail을호출한다() {
        // given
        MailFacade mailFacade = mock(MailFacade.class);
        MailController controller = new MailController(mailFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        MailSendRequest request = new MailSendRequest(
                "\"Sender\" <sender@example.com>",
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
        MailController controller = new MailController(mailFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        UUID messageId = UUID.randomUUID();
        MailSendRequest request = new MailSendRequest(
                "\"Sender\" <sender@example.com>",
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
}
