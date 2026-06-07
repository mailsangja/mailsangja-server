package com.mailsangja.worker.service.notification;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.config.properties.FcmProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmPushCommandServiceTest {

    @Test
    void sendGmailReauthorizationRequestPush_리프레시토큰이없으면발송대상을조회하지않는다() {
        UserDeviceQueryService userDeviceQueryService = mock(UserDeviceQueryService.class);
        FcmPushCommandService service = createService(userDeviceQueryService);
        MailAccount mailAccount = createMailAccount(null);

        service.sendGmailReauthorizationRequestPush(mailAccount);

        verify(userDeviceQueryService, never()).findFcmTokensByMailAccountId(mailAccount.getId());
    }

    @Test
    void sendGmailReauthorizationRequestPush_발송대상이없으면종료한다() {
        UserDeviceQueryService userDeviceQueryService = mock(UserDeviceQueryService.class);
        FcmPushCommandService service = createService(userDeviceQueryService);
        MailAccount mailAccount = createMailAccount("refresh-token");
        when(userDeviceQueryService.findFcmTokensByMailAccountId(mailAccount.getId())).thenReturn(List.of());

        service.sendGmailReauthorizationRequestPush(mailAccount);

        verify(userDeviceQueryService).findFcmTokensByMailAccountId(mailAccount.getId());
    }

    private FcmPushCommandService createService(UserDeviceQueryService userDeviceQueryService) {
        return new FcmPushCommandService(
                userDeviceQueryService,
                mock(UserDeviceCommandService.class),
                null,
                new FcmProperties()
        );
    }

    private MailAccount createMailAccount(String refreshToken) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("gmail@example.com")
                .alias("업무용")
                .icon("good")
                .color("#123456")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 6, 5, 9, 0))
                .refreshToken(refreshToken)
                .active(true)
                .syncHistoryId("history-1")
                .watchExpiresAt(LocalDateTime.of(2026, 6, 6, 9, 0))
                .build();
    }
}
