package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.ReplyDraftSuggestionListResponse;
import com.mailsangja.core.dto.mail.ReplyDraftSuggestionResponse;
import com.mailsangja.core.service.ai.draft.ReplyDraftSuggestionCommandService;
import com.mailsangja.core.service.ai.draft.ReplyDraftSuggestionQueryService;
import com.mailsangja.core.service.mail.MailQueryService;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReplyDraftSuggestionFacadeTest {

    @Test
    void findByMessageId_내메시지면추천초안목록을응답으로반환한다() {
        // given
        User user = createUser(UUID.randomUUID());
        Message message = createMessage(createMailAccount(user, UUID.randomUUID()), UUID.randomUUID());
        ReplyDraftSuggestion first = createSuggestion(message, "승락", "일정 가능합니다", "제안 주신 일정으로 진행하겠습니다.");
        ReplyDraftSuggestion second = createSuggestion(message, "제안", "다른 시간 제안", "다음 주 오전 시간은 어떠실까요?");
        FacadeFixture fixture = createFacade();
        when(fixture.mailQueryService().findReplyTargetMessage(message.getId())).thenReturn(message);
        when(fixture.queryService().findActiveByMessageId(message.getId())).thenReturn(List.of(first, second));

        // when
        ReplyDraftSuggestionListResponse response = fixture.facade().findByMessageId(user, message.getId());

        // then
        assertEquals(2, response.suggestions().size());
        assertEquals(first.getId(), response.suggestions().get(0).id());
        assertEquals("승락", response.suggestions().get(0).type());
        assertEquals("일정 가능합니다", response.suggestions().get(0).subject());
        assertEquals("제안 주신 일정으로 진행하겠습니다.", response.suggestions().get(0).body());
        verify(fixture.mailQueryService()).findReplyTargetMessage(message.getId());
        verify(fixture.queryService()).findActiveByMessageId(message.getId());
        verify(fixture.commandService(), never()).deleteAllByMessageId(any());
    }

    @Test
    void findByMessageId_다른사용자메시지면실패한다() {
        // given
        User user = createUser(UUID.randomUUID());
        User owner = createUser(UUID.randomUUID());
        Message message = createMessage(createMailAccount(owner, UUID.randomUUID()), UUID.randomUUID());
        FacadeFixture fixture = createFacade();
        when(fixture.mailQueryService().findReplyTargetMessage(message.getId())).thenReturn(message);

        // when & then
        assertThrows(MailDraftException.class, () -> fixture.facade().findByMessageId(user, message.getId()));

        // then
        verify(fixture.queryService(), never()).findActiveByMessageId(any());
        verify(fixture.commandService(), never()).deleteAllByMessageId(any());
    }

    @Test
    void select_선택한추천초안을반환하고같은메시지의초안들을삭제한다() {
        // given
        User user = createUser(UUID.randomUUID());
        Message message = createMessage(createMailAccount(user, UUID.randomUUID()), UUID.randomUUID());
        ReplyDraftSuggestion suggestion = createSuggestion(message, "거절", "참석이 어렵습니다", "해당 일정에는 참석이 어렵습니다.");
        FacadeFixture fixture = createFacade();
        when(fixture.queryService().findActiveById(suggestion.getId())).thenReturn(suggestion);

        // when
        ReplyDraftSuggestionResponse response = fixture.facade().select(user, suggestion.getId());

        // then
        assertEquals(suggestion.getId(), response.id());
        assertEquals("거절", response.type());
        assertEquals("참석이 어렵습니다", response.subject());
        assertEquals("해당 일정에는 참석이 어렵습니다.", response.body());
        verify(fixture.queryService()).findActiveById(suggestion.getId());
        verify(fixture.commandService()).deleteAllByMessageId(message.getId());
    }

    @Test
    void select_다른사용자의추천초안이면삭제하지않고실패한다() {
        // given
        User user = createUser(UUID.randomUUID());
        User owner = createUser(UUID.randomUUID());
        Message message = createMessage(createMailAccount(owner, UUID.randomUUID()), UUID.randomUUID());
        ReplyDraftSuggestion suggestion = createSuggestion(message, "승락", "제목", "본문");
        FacadeFixture fixture = createFacade();
        when(fixture.queryService().findActiveById(suggestion.getId())).thenReturn(suggestion);

        // when & then
        assertThrows(MailDraftException.class, () -> fixture.facade().select(user, suggestion.getId()));

        // then
        verify(fixture.commandService(), never()).deleteAllByMessageId(any());
    }

    private FacadeFixture createFacade() {
        MailQueryService mailQueryService = mock(MailQueryService.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        ReplyDraftSuggestionCommandService commandService = mock(ReplyDraftSuggestionCommandService.class);
        return new FacadeFixture(
                new ReplyDraftSuggestionFacade(mailQueryService, queryService, commandService),
                mailQueryService,
                queryService,
                commandService
        );
    }

    private ReplyDraftSuggestion createSuggestion(Message message, String type, String subject, String body) {
        return ReplyDraftSuggestion.builder()
                .id(UUID.randomUUID())
                .message(message)
                .type(type)
                .subject(subject)
                .body(body)
                .build();
    }

    private Message createMessage(MailAccount account, UUID id) {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(account)
                .gmailThreadId(UUID.randomUUID().toString())
                .direction(Direction.INBOUND)
                .build();
        return Message.builder()
                .id(id)
                .thread(thread)
                .gmailMessageId(UUID.randomUUID().toString())
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .build();
    }

    private MailAccount createMailAccount(User user, UUID id) {
        return MailAccount.builder()
                .id(id)
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress("sender@example.com")
                .active(true)
                .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    private User createUser(UUID id) {
        return User.builder().id(id).build();
    }

    private record FacadeFixture(
            ReplyDraftSuggestionFacade facade,
            MailQueryService mailQueryService,
            ReplyDraftSuggestionQueryService queryService,
            ReplyDraftSuggestionCommandService commandService
    ) {
    }
}
