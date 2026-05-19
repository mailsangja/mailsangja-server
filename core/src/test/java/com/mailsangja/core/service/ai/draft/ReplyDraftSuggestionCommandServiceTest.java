package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.ReplyDraftSuggestionRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReplyDraftSuggestionCommandServiceTest {

    @Test
    void deleteAllByMessageId_messageId의활성추천초안을모두삭제한다() {
        // given
        ReplyDraftSuggestionRepositoryPort repositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(repositoryPort, queryService);
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId);
        ReplyDraftSuggestion first = createSuggestion(message, "승락");
        ReplyDraftSuggestion second = createSuggestion(message, "제안");
        when(queryService.findActiveByMessageId(messageId)).thenReturn(List.of(first, second));

        // when
        service.deleteAllByMessageId(messageId);

        // then
        verify(queryService).findActiveByMessageId(messageId);
        verify(repositoryPort).delete(first);
        verify(repositoryPort).delete(second);
    }

    @Test
    void deleteAllByMessageId_활성추천초안이없으면삭제를호출하지않는다() {
        // given
        ReplyDraftSuggestionRepositoryPort repositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(repositoryPort, queryService);
        UUID messageId = UUID.randomUUID();
        when(queryService.findActiveByMessageId(messageId)).thenReturn(List.of());

        // when
        service.deleteAllByMessageId(messageId);

        // then
        verify(queryService).findActiveByMessageId(messageId);
        verify(repositoryPort, never()).delete(any());
    }

    @Test
    void deleteAllByMessageId_messageId가없으면실패한다() {
        // given
        ReplyDraftSuggestionRepositoryPort repositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(repositoryPort, queryService);

        // when & then
        assertThrows(MailDraftException.class, () -> service.deleteAllByMessageId(null));

        // then
        verify(queryService, never()).findActiveByMessageId(any());
        verify(repositoryPort, never()).delete(any());
    }

    private ReplyDraftSuggestion createSuggestion(Message message, String type) {
        return ReplyDraftSuggestion.builder()
                .id(UUID.randomUUID())
                .message(message)
                .type(type)
                .subject(type + " 제목")
                .body(type + " 본문")
                .build();
    }

    private Message createMessage(UUID id) {
        User user = User.builder().id(UUID.randomUUID()).build();
        MailAccount account = MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress("sender@example.com")
                .active(true)
                .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
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
}
