package com.mailsangja.core.service.ai.draft;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailDraftLexicalQueryBuilderTest {

    private final MailDraftLexicalQueryBuilder builder = new MailDraftLexicalQueryBuilder();

    @Test
    void 문장에서안전한OrTsQuery를만든다() {
        // when
        String result = builder.build("김철수 교수님께 수강 정정 가능한지 메일 써줘?", Map.of());

        // then
        assertEquals("김철수 | 교수님께 | 수강 | 정정 | 가능한지 | 메일 | 써줘", result);
    }

    @Test
    void tsquery문법문자는토큰에서제외한다() {
        // when
        String result = builder.build("수강 & 정정 | DROP! (교수)", Map.of());

        // then
        assertEquals("수강 | 정정 | drop | 교수", result);
    }

    @Test
    void 마스킹토큰을복원한뒤검색토큰을만든다() {
        // when
        String result = builder.build("[PERSON_1]에게 [EMAIL_1] 관련 메일 써줘",
                Map.of("[PERSON_1]", "김철수", "[EMAIL_1]", "kim@example.com"));

        // then
        assertEquals("김철수에게 | kim | example | com | 관련 | 메일 | 써줘", result);
    }
}
