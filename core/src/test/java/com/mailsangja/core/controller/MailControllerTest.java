package com.mailsangja.core.controller;

import com.mailsangja.core.dto.mail.MailDraftStreamRequest;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.dto.mail.ReplyDraftSuggestionListResponse;
import com.mailsangja.core.dto.mail.ReplyDraftSuggestionResponse;
import com.mailsangja.core.facade.MailFacade;
import com.mailsangja.core.facade.MailDraftFacade;
import com.mailsangja.core.facade.MailReviewFacade;
import com.mailsangja.core.facade.ReplyDraftSuggestionFacade;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
        ReplyDraftSuggestionFacade replyDraftSuggestionFacade = mock(ReplyDraftSuggestionFacade.class);
        MailController controller = createController(mailFacade, mock(MailDraftFacade.class), replyDraftSuggestionFacade);
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
        verifyNoReplyDraftSuggestion(replyDraftSuggestionFacade);
    }

    @Test
    void sendMail_messageId가있으면replyMail을호출한다() {
        // given
        MailFacade mailFacade = mock(MailFacade.class);
        ReplyDraftSuggestionFacade replyDraftSuggestionFacade = mock(ReplyDraftSuggestionFacade.class);
        MailController controller = createController(mailFacade, mock(MailDraftFacade.class), replyDraftSuggestionFacade);
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
        verifyNoReplyDraftSuggestion(replyDraftSuggestionFacade);
    }

    @Test
    void streamDraft_ResponseEntitySseEmitter를반환한다() {
        // given
        MailFacade mailFacade = mock(MailFacade.class);
        MailDraftFacade mailDraftFacade = mock(MailDraftFacade.class);
        ReplyDraftSuggestionFacade replyDraftSuggestionFacade = mock(ReplyDraftSuggestionFacade.class);
        MailController controller = createController(mailFacade, mailDraftFacade, replyDraftSuggestionFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        MailDraftStreamRequest request = new MailDraftStreamRequest(
                "sender@example.com",
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
        verifyNoReplyDraftSuggestion(replyDraftSuggestionFacade);
    }

    @Test
    void getReplyDraftSuggestions_메시지기준추천초안목록을반환한다() {
        // given
        MailFacade mailFacade = mock(MailFacade.class);
        ReplyDraftSuggestionFacade replyDraftSuggestionFacade = mock(ReplyDraftSuggestionFacade.class);
        MailController controller = createController(mailFacade, mock(MailDraftFacade.class), replyDraftSuggestionFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        UUID messageId = UUID.randomUUID();
        ReplyDraftSuggestionListResponse facadeResponse = new ReplyDraftSuggestionListResponse(List.of(
                new ReplyDraftSuggestionResponse(UUID.randomUUID(), "승락", "일정 가능합니다", "제안 주신 일정으로 진행하겠습니다."),
                new ReplyDraftSuggestionResponse(UUID.randomUUID(), "제안", "다른 시간 제안", "다음 주 오전 시간은 어떠실까요?")
        ));
        when(replyDraftSuggestionFacade.findByMessageId(user, messageId)).thenReturn(facadeResponse);

        // when
        ResponseEntity<ReplyDraftSuggestionListResponse> response = controller.getReplyDraftSuggestions(user, messageId);

        // then
        assertEquals(200, response.getStatusCode().value());
        assertSame(facadeResponse, response.getBody());
        verify(replyDraftSuggestionFacade).findByMessageId(user, messageId);
        verifyNoMailSend(mailFacade);
    }

    @Test
    void selectReplyDraftSuggestion_선택한추천초안을반환한다() {
        // given
        MailFacade mailFacade = mock(MailFacade.class);
        ReplyDraftSuggestionFacade replyDraftSuggestionFacade = mock(ReplyDraftSuggestionFacade.class);
        MailController controller = createController(mailFacade, mock(MailDraftFacade.class), replyDraftSuggestionFacade);
        User user = User.builder().id(UUID.randomUUID()).build();
        UUID suggestionId = UUID.randomUUID();
        ReplyDraftSuggestionResponse facadeResponse = new ReplyDraftSuggestionResponse(
                suggestionId,
                "거절",
                "참석이 어렵습니다",
                "안타깝지만 해당 일정에는 참석이 어렵습니다."
        );
        when(replyDraftSuggestionFacade.select(user, suggestionId)).thenReturn(facadeResponse);

        // when
        ResponseEntity<ReplyDraftSuggestionResponse> response = controller.selectReplyDraftSuggestion(user, suggestionId);

        // then
        assertEquals(200, response.getStatusCode().value());
        assertSame(facadeResponse, response.getBody());
        verify(replyDraftSuggestionFacade).select(user, suggestionId);
        verifyNoMailSend(mailFacade);
    }

    @Test
    void getReplyDraftSuggestions_api경로를가진다() throws Exception {
        // given
        Method method = MailController.class.getMethod("getReplyDraftSuggestions", User.class, UUID.class);

        // when
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        // then
        assertArrayEquals(new String[]{"/api/v1/mail/messages/{messageId}/reply-draft-suggestions"}, mapping.value());
    }

    @Test
    void selectReplyDraftSuggestion_api경로를가진다() throws Exception {
        // given
        Method method = MailController.class.getMethod("selectReplyDraftSuggestion", User.class, UUID.class);

        // when
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        // then
        assertArrayEquals(new String[]{"/api/v1/mail/reply-draft-suggestions/{suggestionId}/select"}, mapping.value());
    }

    private void verifyNoMailSend(MailFacade mailFacade) {
        verify(mailFacade, never()).sendMail(any(), any());
        verify(mailFacade, never()).replyMail(any(), any(), any());
    }

    private void verifyNoReplyDraftSuggestion(ReplyDraftSuggestionFacade replyDraftSuggestionFacade) {
        verify(replyDraftSuggestionFacade, never()).findByMessageId(any(), any());
        verify(replyDraftSuggestionFacade, never()).select(any(), any());
    }

    private MailController createController(
            MailFacade mailFacade,
            MailDraftFacade mailDraftFacade,
            ReplyDraftSuggestionFacade replyDraftSuggestionFacade
    ) {
        return new MailController(mailFacade, mailDraftFacade, mock(MailReviewFacade.class), replyDraftSuggestionFacade);
    }
}
