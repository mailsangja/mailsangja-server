package com.mailsangja.worker.service.ai.masking;

import com.mailsangja.worker.common.exception.masking.MaskingException;
import com.mailsangja.worker.dto.ai.masking.MaskingCommand;
import com.mailsangja.worker.dto.ai.masking.MaskingResult;
import com.mailsangja.worker.dto.ai.masking.MaskingScope;
import com.mailsangja.worker.dto.ai.masking.MaskingTokenResult;
import com.mailsangja.worker.dto.ai.masking.PiiType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                PiiType.PHONE,
                PiiType.URL,
                PiiType.ADDRESS,
                PiiType.ACCOUNT_NUMBER,
                PiiType.CARD_NUMBER,
                PiiType.KOREAN_RRN
        );
        assertThat(detectedTypes).doesNotContain(PiiType.EMAIL, PiiType.PERSON_NAME);
    }

    @Test
    void assignsDeterministicTokensBySourceOffsetAndReusesRepeatedOriginalValue() {
        String text = "first a@test.com second 010-1234-5678 repeat a@test.com";

        MaskingResult result = service.mask(text, MaskingCommand.currentContext());

        assertThat(result.maskedText()).isEqualTo("first a@test.com second [PHONE_1] repeat a@test.com");
        assertThat(result.tokens()).extracting(MaskingTokenResult::token)
                .containsExactly("[PHONE_1]");
        assertThat(result.restoreTokenMap()).containsEntry("[PHONE_1]", "010-1234-5678");
        assertThat(result.redactedTokenMap()).isEmpty();
    }

    @Test
    void putsPastContextTokensOnlyIntoRedactedTokenMap() {
        MaskingResult result = service.mask("past 010-1234-5678", MaskingCommand.pastContext());

        assertThat(result.maskedText()).isEqualTo("past [PHONE_1]");
        assertThat(result.restoreTokenMap()).isEmpty();
        assertThat(result.redactedTokenMap()).containsEntry("[PHONE_1]", "010-1234-5678");
    }

    @Test
    void preservesStringStartInclusiveAndEndExclusiveOffsets() {
        String text = "to 010-1234-5678";

        MaskingResult result = service.mask(text, MaskingCommand.currentContext());

        MaskingTokenResult token = result.tokens().getFirst();
        assertThat(token.startInclusive()).isEqualTo(3);
        assertThat(token.endExclusive()).isEqualTo(16);
        assertThat(text.substring(token.startInclusive(), token.endExclusive())).isEqualTo("010-1234-5678");
    }

    @Test
    void restoresOnlyCurrentContextTokens() {
        MaskingResult result = service.mask("연락처 010-1234-5678", MaskingCommand.currentContext());

        String restored = service.restore("연락처는 [PHONE_1]입니다.", result);

        assertThat(restored).isEqualTo("연락처는 010-1234-5678입니다.");
    }

    @Test
    void doesNotRestorePastContextTokens() {
        MaskingResult result = service.mask("과거 연락처 010-1234-5678", MaskingCommand.pastContext());

        String restored = service.restore("과거 연락처는 [PHONE_1]입니다.", result);

        assertThat(restored).isEqualTo("과거 연락처는 [PHONE_1]입니다.");
    }

    @Test
    void immutableSet입력도Null검사에서예외가발생하지않는다() {
        assertDoesNotThrow(() -> new MaskingCommand(MaskingScope.CURRENT_CONTEXT, Set.of(PiiType.EMAIL)));
    }

    @Test
    void enabledTypes에Null이있으면MaskingException을던진다() {
        Set<PiiType> enabledTypes = new HashSet<>();
        enabledTypes.add(null);

        assertThrows(MaskingException.class, () -> new MaskingCommand(MaskingScope.CURRENT_CONTEXT, enabledTypes));
    }

    @Test
    void immutableList토큰입력도Null검사에서예외가발생하지않는다() {
        MaskingTokenResult token = new MaskingTokenResult(PiiType.EMAIL, "[EMAIL_1]", "a@test.com", 0, 10);

        assertDoesNotThrow(() -> new MaskingResult("masked", List.of(token), Map.of(), Map.of()));
    }

    @Test
    void tokens에Null이있으면MaskingException을던진다() {
        List<MaskingTokenResult> tokens = new java.util.ArrayList<>();
        tokens.add(null);

        assertThrows(MaskingException.class, () -> new MaskingResult("masked", tokens, Map.of(), Map.of()));
    }
}
