package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.MailDraftCommand;
import com.mailsangja.core.dto.mail.MailDraftStreamRequest;
import com.mailsangja.core.service.ai.draft.MailDraftAsyncService;
import com.mailsangja.core.service.ai.draft.MailDraftQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.mail.MailQueryService;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailDraftFacadeTest {

    @Test
    void streamDraft_내활성메일계정이면emitter를반환하고스트리밍을위임한다() {
        // given
        User user = createUser(UUID.randomUUID());
        MailAccount account = createMailAccount(user, UUID.randomUUID());
        FacadeFixture fixture = createFacade(account, null);
        MailDraftStreamRequest request = createRequest(account.getId(), null);

        // when
        SseEmitter emitter = fixture.facade().streamDraft(user, request);

        // then
        assertNotNull(emitter);
        verify(fixture.draftAsyncService()).streamGeneral(eq(emitter), any(MailDraftCommand.class));
    }

    @Test
    void streamDraft_다른사용자메일계정이면실패한다() {
        // given
        User user = createUser(UUID.randomUUID());
        User owner = createUser(UUID.randomUUID());
        MailAccount account = createMailAccount(owner, UUID.randomUUID());
        FacadeFixture fixture = createFacade(account, null);

        // when & then
        assertThrows(MailDraftException.class, () -> fixture.facade().streamDraft(user, createRequest(account.getId(), null)));

        // then
        verify(fixture.draftAsyncService(), never()).streamGeneral(any(), any());
        verify(fixture.draftAsyncService(), never()).streamReply(any(), any());
    }

    @Test
    void streamDraft_replyMessageId가없으면일반초안경로로스트리밍한다() {
        // given
        User user = createUser(UUID.randomUUID());
        MailAccount account = createMailAccount(user, UUID.randomUUID());
        FacadeFixture fixture = createFacade(account, null);

        // when
        fixture.facade().streamDraft(user, createRequest(account.getId(), null));

        // then
        verify(fixture.mailQueryService(), never()).findReplyTargetMessage(any());
        verify(fixture.draftAsyncService()).streamGeneral(any(), any(MailDraftCommand.class));
        verify(fixture.draftAsyncService(), never()).streamReply(any(), any());
    }

    @Test
    void streamDraft_replyMessageId가있으면답장초안경로로스트리밍한다() {
        // given
        User user = createUser(UUID.randomUUID());
        MailAccount account = createMailAccount(user, UUID.randomUUID());
        Message replyTarget = createMessage(account);
        FacadeFixture fixture = createFacade(account, replyTarget);
        UUID replyMessageId = replyTarget.getId();

        // when
        fixture.facade().streamDraft(user, createRequest(account.getId(), replyMessageId));

        // then
        verify(fixture.mailQueryService()).findReplyTargetMessage(replyMessageId);
        verify(fixture.draftAsyncService()).streamReply(any(), any(MailDraftCommand.class));
        verify(fixture.draftAsyncService(), never()).streamGeneral(any(), any());
    }

    @Test
    void streamDraft_replyMessageId가삭제된메시지면실패한다() {
        // given
        User user = createUser(UUID.randomUUID());
        MailAccount account = createMailAccount(user, UUID.randomUUID());
        Message deletedReplyTarget = createMessage(account);
        deletedReplyTarget.delete();
        FacadeFixture fixture = createFacade(account, deletedReplyTarget);

        // when & then
        assertThrows(MailDraftException.class, () -> fixture.facade().streamDraft(user, createRequest(account.getId(), UUID.randomUUID())));

        // then
        verify(fixture.draftAsyncService(), never()).streamGeneral(any(), any());
        verify(fixture.draftAsyncService(), never()).streamReply(any(), any());
    }

    @Test
    void streamDraft_답장대상계정이다르면요청계정기준으로답장초안을작성한다() {
        // given
        User user = createUser(UUID.randomUUID());
        MailAccount requestAccount = createMailAccount(user, UUID.randomUUID());
        MailAccount replyAccount = createMailAccount(user, UUID.randomUUID());
        FacadeFixture fixture = createFacade(requestAccount, createMessage(replyAccount));
        ArgumentCaptor<MailDraftCommand> captor = ArgumentCaptor.forClass(MailDraftCommand.class);

        // when
        fixture.facade().streamDraft(user, createRequest(requestAccount.getId(), UUID.randomUUID()));

        // then
        verify(fixture.draftAsyncService()).streamReply(any(), captor.capture());
        verify(fixture.draftAsyncService(), never()).streamGeneral(any(), any());
        assertEquals(requestAccount.getId(), captor.getValue().mailAccountId());
    }

    @Test
    void streamDraft_promptInjection이면rateLimit을소모하지않고실패한다() {
        // given
        User user = createUser(UUID.randomUUID());
        MailAccount account = createMailAccount(user, UUID.randomUUID());
        FacadeFixture fixture = createFacade(account, null);
        doThrow(mock(MailDraftException.class)).when(fixture.draftQueryService()).validatePromptInjection(any());

        // when & then
        assertThrows(MailDraftException.class, () -> fixture.facade().streamDraft(user, createRequest(account.getId(), null)));

        // then
        verify(fixture.draftQueryService(), atLeastOnce()).validatePromptInjection(any());
        verify(fixture.draftAsyncService(), never()).streamGeneral(any(), any());
    }

    private FacadeFixture createFacade(MailAccount account, Message replyTarget) {
        MailAccountQueryService accountQueryService = mock(MailAccountQueryService.class);
        MailQueryService mailQueryService = mock(MailQueryService.class);
        MailDraftQueryService draftQueryService = mock(MailDraftQueryService.class);
        MailDraftAsyncService draftAsyncService = mock(MailDraftAsyncService.class);
        when(accountQueryService.findActiveById(account.getId())).thenReturn(account);
        when(mailQueryService.findReplyTargetMessage(any())).thenReturn(replyTarget);
        MailDraftFacade facade = new MailDraftFacade(
                accountQueryService,
                mailQueryService,
                draftQueryService,
                draftAsyncService
        );
        return new FacadeFixture(facade, mailQueryService, draftQueryService, draftAsyncService);
    }

    private record FacadeFixture(
            MailDraftFacade facade,
            MailQueryService mailQueryService,
            MailDraftQueryService draftQueryService,
            MailDraftAsyncService draftAsyncService
    ) {
    }

    private MailDraftStreamRequest createRequest(UUID mailAccountId, UUID replyMessageId) {
        return new MailDraftStreamRequest(mailAccountId, "답장 초안을 작성해줘", replyMessageId, List.of("to@example.com"), null);
    }

    private User createUser(UUID id) {
        return User.builder().id(id).build();
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

    private Message createMessage(MailAccount account) {
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
