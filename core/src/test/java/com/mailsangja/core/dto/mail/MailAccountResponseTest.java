package com.mailsangja.core.dto.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailAccountResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void from_Gmail계정이고리프레시토큰이blank이면재연동필요로응답한다() throws Exception {
        // given
        MailAccount mailAccount = createMailAccount(MailProvider.GMAIL, " ");

        // when
        MailAccountResponse response = MailAccountResponse.from(mailAccount);
        String json = objectMapper.writeValueAsString(response);

        // then
        assertTrue(response.reauthorizationRequired());
        assertTrue(json.contains("\"reauthorizationRequired\":true"));
        assertFalse(json.contains("reauthRequired"));
    }

    @Test
    void from_Gmail계정이아니면리프레시토큰이blank여도재연동필요가아니다() {
        // given
        MailAccount mailAccount = createMailAccount(MailProvider.NAVER, " ");

        // when
        MailAccountResponse response = MailAccountResponse.from(mailAccount);
        MailAccountListResponse listResponse = MailAccountListResponse.from(mailAccount);

        // then
        assertFalse(response.reauthorizationRequired());
        assertFalse(listResponse.reauthorizationRequired());
    }

    private MailAccount createMailAccount(MailProvider provider, String refreshToken) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(createUser())
                .provider(provider)
                .emailAddress("mail@example.com")
                .alias("alias")
                .icon("good")
                .color("#123456")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 6, 7, 9, 0))
                .refreshToken(refreshToken)
                .active(true)
                .syncHistoryId("history-id")
                .watchExpiresAt(LocalDateTime.of(2026, 6, 8, 9, 0))
                .build();
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
}
