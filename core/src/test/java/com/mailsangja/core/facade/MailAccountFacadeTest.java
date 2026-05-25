package com.mailsangja.core.facade;

import com.mailsangja.core.dto.contact.GoogleContactResult;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
import com.mailsangja.core.dto.mail.InitialMailSyncMessage;
import com.mailsangja.core.dto.mail.MailAccountAppearanceUpdateRequest;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountListResponse;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.MailAccountProperties;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailAccountFacadeTest {

    @Test
    void authorizeGoogle_인가URL을응답으로반환한다() {
        // given
        MailAccountCommandService mailAccountCommandService = mock(MailAccountCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        GoogleOAuthQueryService googleOAuthQueryService = mock(GoogleOAuthQueryService.class);
        GoogleMailWatchQueryService googleMailWatchQueryService = mock(GoogleMailWatchQueryService.class);
        InitialMailSyncMessageCommandService initialMailSyncMessageCommandService = mock(InitialMailSyncMessageCommandService.class);
        GooglePeopleContactQueryService googlePeopleContactQueryService = mock(GooglePeopleContactQueryService.class);
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        MailAccountFacade facade = new MailAccountFacade(
                mailAccountProperties(),
                mailAccountCommandService,
                mailAccountQueryService,
                googleOAuthQueryService,
                googleMailWatchQueryService,
                initialMailSyncMessageCommandService,
                googlePeopleContactQueryService,
                contactCommandService
        );
        when(googleOAuthQueryService.buildAuthorizationUrl("state-123"))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth");

        // when
        MailAccountAuthorizeResponse response = facade.authorizeGoogle("state-123");

        // then
        assertEquals("https://accounts.google.com/o/oauth2/v2/auth", response.authorizationUrl());
    }

    @Test
    void handleGoogleCallback_메일계정저장후MQ를먼저발행하고그다음주소록을가져와저장한다() {
        // given
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
                mailAccountProperties(),
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

        // when
        MailAccountResponse response = facade.handleGoogleCallback(user, "code", "alias", null, null);

        // then
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
        // given
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
                mailAccountProperties(),
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

        // when
        MailAccountResponse response = facade.handleGoogleCallback(user, "code", "alias", null, null);

        // then
        assertEquals(savedMailAccount.getId(), response.id());
        verify(initialMailSyncMessageCommandService).publish(InitialMailSyncMessage.from(savedMailAccount));
        verify(googlePeopleContactQueryService).getContacts("access-token");
    }

    @Test
    void handleGoogleCallback_주소록저장이실패해도메일계정연동과초기메일동기화발행은유지한다() {
        // given
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
                mailAccountProperties(),
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

        // when
        MailAccountResponse response = facade.handleGoogleCallback(user, "code", "alias", null, null);

        // then
        assertEquals(savedMailAccount.getId(), response.id());
        verify(initialMailSyncMessageCommandService).publish(InitialMailSyncMessage.from(savedMailAccount));
        verify(contactCommandService).saveMissingContacts(user, contacts);
    }

    @Test
    void handleGoogleCallback_인가코드가잘못되면예외를던진다() {
        // given
        MailAccountFacade facade = createFacade(
                mock(MailAccountCommandService.class),
                mock(MailAccountQueryService.class),
                mock(GoogleOAuthQueryService.class),
                mock(GoogleMailWatchQueryService.class),
                mock(InitialMailSyncMessageCommandService.class),
                mock(GooglePeopleContactQueryService.class),
                mock(ContactCommandService.class)
        );

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> facade.handleGoogleCallback(createUser(), "invalid code", "alias", "good", "#123456")
        );

        // then
        assertEquals(MailAccountErrorCode.INVALID_AUTHORIZATION_CODE, exception.getErrorCode());
    }

    @Test
    void handleGoogleCallback_표시값이없으면이메일별칭과기본아이콘과랜덤색상으로생성한다() {
        // given
        User user = createUser();
        GoogleMailAccountResult accountResult = createAccountResult();
        GoogleMailWatchResult watchResult = createWatchResult();
        MailAccountCommandService mailAccountCommandService = mock(MailAccountCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        GoogleOAuthQueryService googleOAuthQueryService = mock(GoogleOAuthQueryService.class);
        GoogleMailWatchQueryService googleMailWatchQueryService = mock(GoogleMailWatchQueryService.class);
        InitialMailSyncMessageCommandService initialMailSyncMessageCommandService = mock(InitialMailSyncMessageCommandService.class);
        GooglePeopleContactQueryService googlePeopleContactQueryService = mock(GooglePeopleContactQueryService.class);
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        MailAccountFacade facade = createFacade(
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
                eq("gmail@example.com"),
                eq("good"),
                any(),
                eq(watchResult)
        )).thenReturn(createMailAccount(user));
        when(googlePeopleContactQueryService.getContacts("access-token")).thenReturn(List.of());

        // when
        MailAccountResponse response = facade.handleGoogleCallback(user, "code", null, null, null);

        // then
        assertNotNull(response);
        verify(mailAccountCommandService).createGoogleMailAccount(
                eq(user),
                eq(accountResult),
                eq("gmail@example.com"),
                eq("good"),
                org.mockito.ArgumentMatchers.matches("^#[0-9A-F]{6}$"),
                eq(watchResult)
        );
    }

    @Test
    void handleGoogleCallback_색상형식이잘못되면예외를던진다() {
        // given
        GoogleOAuthQueryService googleOAuthQueryService = mock(GoogleOAuthQueryService.class);
        MailAccountFacade facade = createFacade(
                mock(MailAccountCommandService.class),
                mock(MailAccountQueryService.class),
                googleOAuthQueryService,
                mock(GoogleMailWatchQueryService.class),
                mock(InitialMailSyncMessageCommandService.class),
                mock(GooglePeopleContactQueryService.class),
                mock(ContactCommandService.class)
        );
        when(googleOAuthQueryService.getGoogleMailAccountResult("code")).thenReturn(createAccountResult());

        // when
        MailAccountException exception = assertThrows(
                MailAccountException.class,
                () -> facade.handleGoogleCallback(createUser(), "code", "alias", "good", "123456")
        );

        // then
        assertEquals(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_COLOR, exception.getErrorCode());
    }

    @Test
    void getMyMailAccounts_사용자메일계정목록을응답으로변환한다() {
        // given
        User user = createUser();
        MailAccountCommandService mailAccountCommandService = mock(MailAccountCommandService.class);
        MailAccountQueryService mailAccountQueryService = mock(MailAccountQueryService.class);
        MailAccountFacade facade = createFacade(
                mailAccountCommandService,
                mailAccountQueryService,
                mock(GoogleOAuthQueryService.class),
                mock(GoogleMailWatchQueryService.class),
                mock(InitialMailSyncMessageCommandService.class),
                mock(GooglePeopleContactQueryService.class),
                mock(ContactCommandService.class)
        );
        when(mailAccountQueryService.findAllByUserId(user.getId()))
                .thenReturn(List.of(createMailAccount(user)));

        // when
        List<MailAccountListResponse> responses = facade.getMyMailAccounts(user);

        // then
        assertEquals(1, responses.size());
        assertEquals("gmail@example.com", responses.getFirst().emailAddress());
    }

    @Test
    void updateMailAccountAppearance_요청검증후서비스에위임한다() {
        // given
        User user = createUser();
        UUID mailAccountId = UUID.randomUUID();
        MailAccountCommandService mailAccountCommandService = mock(MailAccountCommandService.class);
        MailAccountFacade facade = createFacade(
                mailAccountCommandService,
                mock(MailAccountQueryService.class),
                mock(GoogleOAuthQueryService.class),
                mock(GoogleMailWatchQueryService.class),
                mock(InitialMailSyncMessageCommandService.class),
                mock(GooglePeopleContactQueryService.class),
                mock(ContactCommandService.class)
        );
        MailAccountAppearanceUpdateRequest request = new MailAccountAppearanceUpdateRequest("alias", "good", "#123456");

        // when
        facade.updateMailAccountAppearance(user, mailAccountId, request);

        // then
        verify(mailAccountCommandService).updateMailAccountAppearance(user, mailAccountId, "alias", "good", "#123456");
    }

    @Test
    void deactivateMailAccount_서비스에위임한다() {
        // given
        User user = createUser();
        UUID mailAccountId = UUID.randomUUID();
        MailAccountCommandService mailAccountCommandService = mock(MailAccountCommandService.class);
        MailAccountFacade facade = createFacade(
                mailAccountCommandService,
                mock(MailAccountQueryService.class),
                mock(GoogleOAuthQueryService.class),
                mock(GoogleMailWatchQueryService.class),
                mock(InitialMailSyncMessageCommandService.class),
                mock(GooglePeopleContactQueryService.class),
                mock(ContactCommandService.class)
        );

        // when
        facade.deactivateMailAccount(user, mailAccountId);

        // then
        verify(mailAccountCommandService).deactivateMailAccount(user, mailAccountId);
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

    private MailAccountFacade createFacade(
            MailAccountCommandService mailAccountCommandService,
            MailAccountQueryService mailAccountQueryService,
            GoogleOAuthQueryService googleOAuthQueryService,
            GoogleMailWatchQueryService googleMailWatchQueryService,
            InitialMailSyncMessageCommandService initialMailSyncMessageCommandService,
            GooglePeopleContactQueryService googlePeopleContactQueryService,
            ContactCommandService contactCommandService
    ) {
        return new MailAccountFacade(
                mailAccountProperties(),
                mailAccountCommandService,
                mailAccountQueryService,
                googleOAuthQueryService,
                googleMailWatchQueryService,
                initialMailSyncMessageCommandService,
                googlePeopleContactQueryService,
                contactCommandService
        );
    }

    private MailAccountProperties mailAccountProperties() {
        MailAccountProperties properties = new MailAccountProperties();
        properties.setMaxCount(2);
        return properties;
    }
}
