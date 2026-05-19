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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReplyDraftSuggestionQueryServiceTest {

    @Test
    void findActiveByMessageId_messageId로활성추천초안을조회한다() {
        // given
        ReplyDraftSuggestionRepositoryPort repositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService service = new ReplyDraftSuggestionQueryService(repositoryPort);
        UUID messageId = UUID.randomUUID();
        List<ReplyDraftSuggestion> suggestions = List.of(createSuggestion(createMessage(messageId)));
        when(repositoryPort.findAllByMessageIdAndDeletedAtIsNull(messageId)).thenReturn(suggestions);

        // when
        List<ReplyDraftSuggestion> result = service.findActiveByMessageId(messageId);

        // then
        assertSame(suggestions, result);
        verify(repositoryPort).findAllByMessageIdAndDeletedAtIsNull(messageId);
    }

    @Test
    void findActiveByMessageId_messageId가없으면실패한다() {
        // given
        ReplyDraftSuggestionRepositoryPort repositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService service = new ReplyDraftSuggestionQueryService(repositoryPort);

        // when & then
        assertThrows(MailDraftException.class, () -> service.findActiveByMessageId(null));
    }

    @Test
    void findActiveById_id로활성추천초안을조회한다() {
        // given
        ReplyDraftSuggestionRepositoryPort repositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService service = new ReplyDraftSuggestionQueryService(repositoryPort);
        UUID suggestionId = UUID.randomUUID();
        ReplyDraftSuggestion suggestion = createSuggestion(createMessage(UUID.randomUUID()));
        when(repositoryPort.findByIdAndDeletedAtIsNull(suggestionId)).thenReturn(Optional.of(suggestion));

        // when
        ReplyDraftSuggestion result = service.findActiveById(suggestionId);

        // then
        assertSame(suggestion, result);
        verify(repositoryPort).findByIdAndDeletedAtIsNull(suggestionId);
    }

    @Test
    void findActiveById_활성추천초안이없으면실패한다() {
        // given
        ReplyDraftSuggestionRepositoryPort repositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService service = new ReplyDraftSuggestionQueryService(repositoryPort);
        UUID suggestionId = UUID.randomUUID();
        when(repositoryPort.findByIdAndDeletedAtIsNull(suggestionId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(MailDraftException.class, () -> service.findActiveById(suggestionId));
    }

    private ReplyDraftSuggestion createSuggestion(Message message) {
        return ReplyDraftSuggestion.builder()
                .id(UUID.randomUUID())
                .message(message)
                .type("승락")
                .subject("일정 가능합니다")
                .body("제안 주신 일정으로 진행하겠습니다.")
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
