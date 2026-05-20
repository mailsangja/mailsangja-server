package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftException;
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

class ReplyDraftSuggestionResponseTest {

    @Test
    void from_엔티티를응답으로변환한다() {
        // given
        ReplyDraftSuggestion suggestion = ReplyDraftSuggestion.builder()
                .id(UUID.randomUUID())
                .message(createMessage())
                .type("승락")
                .subject("일정 가능합니다")
                .body("제안 주신 일정으로 진행하겠습니다.")
                .build();

        // when
        ReplyDraftSuggestionResponse response = ReplyDraftSuggestionResponse.from(suggestion);

        // then
        assertEquals(suggestion.getId(), response.id());
        assertEquals("승락", response.type());
        assertEquals("일정 가능합니다", response.subject());
        assertEquals("제안 주신 일정으로 진행하겠습니다.", response.body());
    }

    @Test
    void from_엔티티가없으면실패한다() {
        // given
        ReplyDraftSuggestion suggestion = null;

        // when & then
        assertThrows(MailDraftException.class, () -> ReplyDraftSuggestionResponse.from(suggestion));
    }

    @Test
    void listOf_엔티티목록을응답목록으로변환한다() {
        // given
        Message message = createMessage();
        List<ReplyDraftSuggestion> suggestions = List.of(
                createSuggestion(message, "승락"),
                createSuggestion(message, "제안")
        );

        // when
        ReplyDraftSuggestionListResponse response = ReplyDraftSuggestionListResponse.from(suggestions);

        // then
        assertEquals(2, response.suggestions().size());
        assertEquals("승락", response.suggestions().get(0).type());
        assertEquals("제안", response.suggestions().get(1).type());
    }

    @Test
    void listOf_null목록이면빈목록으로변환한다() {
        // given
        List<ReplyDraftSuggestion> suggestions = null;

        // when
        ReplyDraftSuggestionListResponse response = ReplyDraftSuggestionListResponse.from(suggestions);

        // then
        assertEquals(0, response.suggestions().size());
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

    private Message createMessage() {
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
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(UUID.randomUUID().toString())
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .build();
    }
}
