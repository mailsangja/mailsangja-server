package com.mailsangja.core.service.ai.masking;

import com.mailsangja.core.dto.ai.masking.MaskingCommand;
import com.mailsangja.core.dto.ai.masking.MaskingResult;
import com.mailsangja.core.dto.ai.masking.MaskingTokenResult;
import com.mailsangja.core.dto.ai.masking.PiiType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PhileasMaskingServiceTest {

    private final PhileasMaskingService service = new PhileasMaskingService();

    @Test
    void detectsPiiInKoreanAndEnglishFixture() {
        String text = """
                Dear Alice Kim,
                담당: 김민수님
                이메일 alice@example.com, 전화 010-1234-5678
                링크 https://example.com/docs
                주소 서울특별시 강남구 테헤란로 123
                계좌 국민 123-456-789012
                카드 1111-2222-3333-4444
                주민번호 900101-1234567
                """;

        MaskingResult result = service.mask(text, MaskingCommand.currentContext());

        Set<PiiType> detectedTypes = result.tokens().stream()
                .map(MaskingTokenResult::piiType)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(detectedTypes).contains(
                PiiType.EMAIL,
                PiiType.PHONE,
                PiiType.URL,
                PiiType.ADDRESS,
                PiiType.ACCOUNT_NUMBER,
                PiiType.CARD_NUMBER,
                PiiType.KOREAN_RRN,
                PiiType.PERSON_NAME
        );
    }

    @Test
    void assignsDeterministicTokensBySourceOffsetAndReusesRepeatedOriginalValue() {
        String text = "first a@test.com second 010-1234-5678 repeat a@test.com";

        MaskingResult result = service.mask(text, MaskingCommand.currentContext());

        assertThat(result.maskedText()).isEqualTo("first [EMAIL_1] second [PHONE_1] repeat [EMAIL_1]");
        assertThat(result.tokens()).extracting(MaskingTokenResult::token)
                .containsExactly("[EMAIL_1]", "[PHONE_1]", "[EMAIL_1]");
        assertThat(result.restoreTokenMap()).containsEntry("[EMAIL_1]", "a@test.com");
        assertThat(result.redactedTokenMap()).isEmpty();
    }

    @Test
    void putsPastContextTokensOnlyIntoRedactedTokenMap() {
        MaskingResult result = service.mask("past user@example.com", MaskingCommand.pastContext());

        assertThat(result.maskedText()).isEqualTo("past [EMAIL_1]");
        assertThat(result.restoreTokenMap()).isEmpty();
        assertThat(result.redactedTokenMap()).containsEntry("[EMAIL_1]", "user@example.com");
    }

    @Test
    void preservesStringStartInclusiveAndEndExclusiveOffsets() {
        String text = "to kim@example.com";

        MaskingResult result = service.mask(text, MaskingCommand.currentContext());

        MaskingTokenResult token = result.tokens().getFirst();
        assertThat(token.startInclusive()).isEqualTo(3);
        assertThat(token.endExclusive()).isEqualTo(18);
        assertThat(text.substring(token.startInclusive(), token.endExclusive())).isEqualTo("kim@example.com");
    }

    @Test
    void restoresOnlyCurrentContextTokens() {
        MaskingResult result = service.mask("수신자 alice@example.com", MaskingCommand.currentContext());

        String restored = service.restore("메일은 [EMAIL_1]에게 보냅니다.", result);

        assertThat(restored).isEqualTo("메일은 alice@example.com에게 보냅니다.");
    }

    @Test
    void doesNotRestorePastContextTokens() {
        MaskingResult result = service.mask("과거 발신자 alice@example.com", MaskingCommand.pastContext());

        String restored = service.restore("과거 발신자는 [EMAIL_1]입니다.", result);

        assertThat(restored).isEqualTo("과거 발신자는 [EMAIL_1]입니다.");
    }
}
