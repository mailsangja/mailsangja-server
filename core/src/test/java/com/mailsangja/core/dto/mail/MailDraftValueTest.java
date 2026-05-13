package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailDraftValueTest {

    @Test
    void command는사용자Id가없으면생성하지않는다() {
        // given
        UUID userId = null;

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftCommand(
                userId, UUID.randomUUID(), "query", null, null, null
        ));
    }

    @Test
    void restoreContext는null토큰맵을빈맵으로정규화한다() {
        // given
        Map<String, String> tokens = null;

        // when
        MailDraftRestoreContextResult result = new MailDraftRestoreContextResult(tokens);

        // then
        assertEquals(Map.of(), result.tokens());
    }

    @Test
    void searchContext는본문이null이면빈문자열로정규화한다() {
        // given
        String body = null;

        // when
        MailDraftSearchContextResult result = new MailDraftSearchContextResult(UUID.randomUUID(), "source", "subject", body);

        // then
        assertEquals("", result.body());
    }

    @Test
    void promptResult는userPrompt가비어있으면생성하지않는다() {
        // given
        String userPrompt = "";

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftPromptResult("system", userPrompt));
    }
}
