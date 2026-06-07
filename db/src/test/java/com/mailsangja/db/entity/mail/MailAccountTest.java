package com.mailsangja.db.entity.mail;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailAccountTest {

    @Test
    void updateAccessToken_액세스토큰을갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        mailAccount.updateAccessToken("new-access-token");

        // then
        assertEquals("new-access-token", mailAccount.getAccessToken());
    }

    @Test
    void updateAccessTokenExpiresAt_액세스토큰만료시각을갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();
        LocalDateTime newExpiresAt = LocalDateTime.of(2026, 5, 19, 10, 30);

        // when
        mailAccount.updateAccessTokenExpiresAt(newExpiresAt);

        // then
        assertEquals(newExpiresAt, mailAccount.getAccessTokenExpiresAt());
    }

    @Test
    void updateRefreshToken_리프레시토큰을갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        mailAccount.updateRefreshToken("new-refresh-token");

        // then
        assertEquals("new-refresh-token", mailAccount.getRefreshToken());
    }

    @Test
    void updateGoogleAuthorizationTokens_토큰필드만갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();
        LocalDateTime originalWatchExpiresAt = mailAccount.getWatchExpiresAt();
        String originalSyncHistoryId = mailAccount.getSyncHistoryId();
        LocalDateTime newAccessTokenExpiresAt = LocalDateTime.of(2026, 5, 19, 11, 0);

        // when
        mailAccount.updateGoogleAuthorizationTokens(
                "new-access-token",
                newAccessTokenExpiresAt,
                "new-refresh-token"
        );

        // then
        assertEquals("new-access-token", mailAccount.getAccessToken());
        assertEquals(newAccessTokenExpiresAt, mailAccount.getAccessTokenExpiresAt());
        assertEquals("new-refresh-token", mailAccount.getRefreshToken());
        assertEquals(originalSyncHistoryId, mailAccount.getSyncHistoryId());
        assertEquals(originalWatchExpiresAt, mailAccount.getWatchExpiresAt());
        assertTrue(mailAccount.isActive());
    }

    @Test
    void updateAlias_별칭을갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        mailAccount.updateAlias("업무용 Gmail");

        // then
        assertEquals("업무용 Gmail", mailAccount.getAlias());
    }

    @Test
    void updateIcon_아이콘을갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        mailAccount.updateIcon("briefcase");

        // then
        assertEquals("briefcase", mailAccount.getIcon());
    }

    @Test
    void updateColor_색상을갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        mailAccount.updateColor("#ABCDEF");

        // then
        assertEquals("#ABCDEF", mailAccount.getColor());
    }

    @Test
    void updateSyncHistoryId_동기화히스토리Id를갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        mailAccount.updateSyncHistoryId("new-history-id");

        // then
        assertEquals("new-history-id", mailAccount.getSyncHistoryId());
    }

    @Test
    void updateWatchExpiresAt_watch만료시각을갱신한다() {
        // given
        MailAccount mailAccount = createMailAccount();
        LocalDateTime newWatchExpiresAt = LocalDateTime.of(2026, 5, 20, 11, 0);

        // when
        mailAccount.updateWatchExpiresAt(newWatchExpiresAt);

        // then
        assertEquals(newWatchExpiresAt, mailAccount.getWatchExpiresAt());
    }

    @Test
    void activate_메일계정을활성화한다() {
        // given
        MailAccount mailAccount = createMailAccount();
        mailAccount.deactivate();

        // when
        mailAccount.activate();

        // then
        assertTrue(mailAccount.isActive());
    }

    @Test
    void deactivate_메일계정을비활성화한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        mailAccount.deactivate();

        // then
        assertFalse(mailAccount.isActive());
    }

    @Test
    void clearRefreshToken_리프레시토큰만삭제하고활성상태는유지한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        mailAccount.clearRefreshToken();

        // then
        assertEquals(null, mailAccount.getRefreshToken());
        assertTrue(mailAccount.isActive());
    }

    @Test
    void resolveStartHistoryId_syncHistoryId가있으면해당값을반환한다() {
        // given
        MailAccount mailAccount = createMailAccount();

        // when
        String startHistoryId = mailAccount.resolveStartHistoryId("fallback-history-id");

        // then
        assertEquals("sync-history-id", startHistoryId);
    }

    @Test
    void resolveStartHistoryId_syncHistoryId가null이면fallback을반환한다() {
        // given
        MailAccount mailAccount = createMailAccount();
        mailAccount.updateSyncHistoryId(null);

        // when
        String startHistoryId = mailAccount.resolveStartHistoryId("fallback-history-id");

        // then
        assertEquals("fallback-history-id", startHistoryId);
    }

    @Test
    void resolveStartHistoryId_syncHistoryId가blank이면fallback을반환한다() {
        // given
        MailAccount mailAccount = createMailAccount();
        mailAccount.updateSyncHistoryId("   ");

        // when
        String startHistoryId = mailAccount.resolveStartHistoryId("fallback-history-id");

        // then
        assertEquals("fallback-history-id", startHistoryId);
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("alias")
                .icon("good")
                .color("#123456")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 5, 19, 9, 0))
                .refreshToken("refresh-token")
                .active(true)
                .syncHistoryId("sync-history-id")
                .watchExpiresAt(LocalDateTime.of(2026, 5, 20, 9, 0))
                .build();
    }
}
