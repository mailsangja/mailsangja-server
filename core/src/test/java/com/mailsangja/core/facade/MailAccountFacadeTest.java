package com.mailsangja.core.facade;

import com.mailsangja.core.dto.contact.GoogleContactResult;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
import com.mailsangja.core.dto.mail.InitialMailSyncMessage;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.core.service.contact.ContactCommandService;
import com.mailsangja.core.service.google.GoogleMailWatchQueryService;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.core.service.google.GooglePeopleContactQueryService;
import com.mailsangja.core.service.mail.InitialMailSyncMessageCommandService;
import com.mailsangja.core.service.mail.MailAccountCommandService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailAccountFacadeTest {

    @Test
    void handleGoogleCallback_메일계정저장후MQ를먼저발행하고그다음주소록을가져와저장한다() {
        User user = createUser();
        GoogleMailAccountResult accountResult = createAccountResult();
        GoogleMailWatchResult watchResult = createWatchResult();
        MailAccount savedMailAccount = createMailAccount(user);
        List<GoogleContactResult> contacts = List.of(new GoogleContactResult("Alice", "alice@example.com"));

        MailAccountCommandService mailAccountCommandService = mock(MailAccountCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        GoogleOAuthQueryService googleOAuthQueryService = mock(GoogleOAuthQueryService.class);
        GoogleMailWatchQueryService googleMailWatchQueryService = mock(GoogleMailWatchQueryService.class);
        InitialMailSyncMessageCommandService initialMailSyncMessageCommandService = mock(InitialMailSyncMessageCommandService.class);
        GooglePeopleContactQueryService googlePeopleContactQueryService = mock(GooglePeopleContactQueryService.class);
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        MailAccountFacade facade = new MailAccountFacade(
                mailAccountCommandService,
                mailAccountQueryService,
                googleOAuthQueryService,
                googleMailWatchQueryService,
                initialMailSyncMessageCommandService,
                googlePeopleContactQueryService,
                contactCommandService
        );
        when(googleOAuthQueryService.getGoogleMailAccountResult("code")).thenReturn(accountResult);
        when(googleMailWatchQueryService.watch("access-token")).thenReturn(watchResult);
        when(mailAccountCommandService.createGoogleMailAccount(
                eq(user),
                eq(accountResult),
                eq("alias"),
                eq("good"),
                any(),
                eq(watchResult)
        )).thenReturn(savedMailAccount);
        when(googlePeopleContactQueryService.getContacts("access-token")).thenReturn(contacts);

        MailAccountResponse response = facade.handleGoogleCallback(user, "code", "alias", null, null);

        assertEquals(savedMailAccount.getId(), response.id());
        InOrder inOrder = inOrder(
                initialMailSyncMessageCommandService,
                googlePeopleContactQueryService,
                contactCommandService
        );
        inOrder.verify(initialMailSyncMessageCommandService).publish(InitialMailSyncMessage.from(savedMailAccount));
        inOrder.verify(googlePeopleContactQueryService).getContacts("access-token");
        inOrder.verify(contactCommandService).saveMissingContacts(user, contacts);
    }

    @Test
    void handleGoogleCallback_주소록조회가실패해도메일계정연동과초기메일동기화발행은유지한다() {
        User user = createUser();
        GoogleMailAccountResult accountResult = createAccountResult();
        GoogleMailWatchResult watchResult = createWatchResult();
        MailAccount savedMailAccount = createMailAccount(user);

        MailAccountCommandService mailAccountCommandService = mock(MailAccountCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        GoogleOAuthQueryService googleOAuthQueryService = mock(GoogleOAuthQueryService.class);
        GoogleMailWatchQueryService googleMailWatchQueryService = mock(GoogleMailWatchQueryService.class);
        InitialMailSyncMessageCommandService initialMailSyncMessageCommandService = mock(InitialMailSyncMessageCommandService.class);
        GooglePeopleContactQueryService googlePeopleContactQueryService = mock(GooglePeopleContactQueryService.class);
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        MailAccountFacade facade = new MailAccountFacade(
                mailAccountCommandService,
                mailAccountQueryService,
                googleOAuthQueryService,
                googleMailWatchQueryService,
                initialMailSyncMessageCommandService,
                googlePeopleContactQueryService,
                contactCommandService
        );
        when(googleOAuthQueryService.getGoogleMailAccountResult("code")).thenReturn(accountResult);
        when(googleMailWatchQueryService.watch("access-token")).thenReturn(watchResult);
        when(mailAccountCommandService.createGoogleMailAccount(
                eq(user),
                eq(accountResult),
                eq("alias"),
                eq("good"),
                any(),
                eq(watchResult)
        )).thenReturn(savedMailAccount);
        doThrow(new RuntimeException("People API failed"))
                .when(googlePeopleContactQueryService)
                .getContacts("access-token");

        MailAccountResponse response = facade.handleGoogleCallback(user, "code", "alias", null, null);

        assertEquals(savedMailAccount.getId(), response.id());
        verify(initialMailSyncMessageCommandService).publish(InitialMailSyncMessage.from(savedMailAccount));
        verify(googlePeopleContactQueryService).getContacts("access-token");
    }

    @Test
    void handleGoogleCallback_주소록저장이실패해도메일계정연동과초기메일동기화발행은유지한다() {
        User user = createUser();
        GoogleMailAccountResult accountResult = createAccountResult();
        GoogleMailWatchResult watchResult = createWatchResult();
        MailAccount savedMailAccount = createMailAccount(user);
        List<GoogleContactResult> contacts = List.of(new GoogleContactResult("Alice", "alice@example.com"));

        MailAccountCommandService mailAccountCommandService = mock(MailAccountCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        GoogleOAuthQueryService googleOAuthQueryService = mock(GoogleOAuthQueryService.class);
        GoogleMailWatchQueryService googleMailWatchQueryService = mock(GoogleMailWatchQueryService.class);
        InitialMailSyncMessageCommandService initialMailSyncMessageCommandService = mock(InitialMailSyncMessageCommandService.class);
        GooglePeopleContactQueryService googlePeopleContactQueryService = mock(GooglePeopleContactQueryService.class);
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        MailAccountFacade facade = new MailAccountFacade(
                mailAccountCommandService,
                mailAccountQueryService,
                googleOAuthQueryService,
                googleMailWatchQueryService,
                initialMailSyncMessageCommandService,
                googlePeopleContactQueryService,
                contactCommandService
        );
        when(googleOAuthQueryService.getGoogleMailAccountResult("code")).thenReturn(accountResult);
        when(googleMailWatchQueryService.watch("access-token")).thenReturn(watchResult);
        when(mailAccountCommandService.createGoogleMailAccount(
                eq(user),
                eq(accountResult),
                eq("alias"),
                eq("good"),
                any(),
                eq(watchResult)
        )).thenReturn(savedMailAccount);
        when(googlePeopleContactQueryService.getContacts("access-token")).thenReturn(contacts);
        doThrow(new RuntimeException("contact save failed"))
                .when(contactCommandService)
                .saveMissingContacts(user, contacts);

        MailAccountResponse response = facade.handleGoogleCallback(user, "code", "alias", null, null);

        assertEquals(savedMailAccount.getId(), response.id());
        verify(initialMailSyncMessageCommandService).publish(InitialMailSyncMessage.from(savedMailAccount));
        verify(contactCommandService).saveMissingContacts(user, contacts);
    }

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("사용자")
                .username("user@example.com")
                .password("password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .build();
    }

    private GoogleMailAccountResult createAccountResult() {
        return new GoogleMailAccountResult(
                "gmail@example.com",
                "access-token",
                LocalDateTime.now().plusHours(1),
                "refresh-token"
        );
    }

    private GoogleMailWatchResult createWatchResult() {
        return new GoogleMailWatchResult("history-id", LocalDateTime.now().plusDays(1));
    }

    private MailAccount createMailAccount(User user) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress("gmail@example.com")
                .alias("alias")
                .icon("good")
                .color("#123456")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .refreshToken("refresh-token")
                .active(true)
                .syncHistoryId("history-id")
                .watchExpiresAt(LocalDateTime.now().plusDays(1))
                .build();
    }
}
