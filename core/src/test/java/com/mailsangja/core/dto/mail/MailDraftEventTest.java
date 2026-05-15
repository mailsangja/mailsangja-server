package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailDraftException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MailDraftEventTest {

    @Test
    void deltaEvent는phase가없으면생성하지않는다() {
        // given
        MailDraftPhase phase = null;

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftDeltaEvent(phase, "delta"));
    }

    @Test
    void usageResult는음수토큰이면생성하지않는다() {
        // given
        int inputTokens = -1;

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftUsageResult("model", inputTokens, 0, 0));
    }

    @Test
    void usageEvent는사용량이없으면생성하지않는다() {
        // given
        MailDraftUsageResult subjectUsage = null;
        MailDraftUsageResult bodyUsage = new MailDraftUsageResult("model", 1, 1, 2);

        // when & then
        assertThrows(MailDraftException.class, () -> MailDraftUsageEvent.of(subjectUsage, bodyUsage));
    }

    @Test
    void doneEvent는상태가비어있으면생성하지않는다() {
        // given
        String status = " ";

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftDoneEvent(status));
    }

    @Test
    void errorEvent는코드가비어있으면생성하지않는다() {
        // given
        String code = "";

        // when & then
        assertThrows(MailDraftException.class, () -> new MailDraftErrorEvent(code, "message"));
    }
}
