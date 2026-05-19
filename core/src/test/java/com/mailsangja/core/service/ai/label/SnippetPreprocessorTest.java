package com.mailsangja.core.service.ai.label;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnippetPreprocessorTest {

    private final SnippetPreprocessor preprocessor = new SnippetPreprocessor();

    @Test
    void null입력시null반환() {
        assertThat(preprocessor.process(null)).isNull();
    }

    @Test
    void PII없는텍스트는변경없음() {
        String input = "안녕하세요. 지난번에 보내주신 자료 잘 받았습니다.";

        assertThat(preprocessor.process(input)).isEqualTo(input);
    }

    @Test
    void 이메일주소는마스킹하지않는다() {
        String input = "문의: support@example.com 으로 연락주세요.";

        assertThat(preprocessor.process(input)).isEqualTo(input);
    }

    @Test
    void 전화번호를마스킹한다() {
        assertThat(preprocessor.process("연락처: 010-1234-5678 입니다."))
                .isEqualTo("연락처: [PHONE] 입니다.");
    }

    @Test
    void 하이픈없는전화번호를마스킹한다() {
        assertThat(preprocessor.process("전화 01012345678 주세요"))
                .isEqualTo("전화 [PHONE] 주세요");
    }

    @Test
    void 주민번호를마스킹한다() {
        assertThat(preprocessor.process("주민번호: 900101-1234567"))
                .isEqualTo("주민번호: [RRN]");
    }

    @Test
    void 하이픈없는주민번호를마스킹한다() {
        assertThat(preprocessor.process("주민번호 9001011234567 확인"))
                .isEqualTo("주민번호 [RRN] 확인");
    }

    @Test
    void 카드번호를마스킹한다() {
        assertThat(preprocessor.process("카드: 1234-5678-9012-3456"))
                .isEqualTo("카드: [CARD]");
    }

    @Test
    void 공백구분카드번호를마스킹한다() {
        assertThat(preprocessor.process("결제 카드 1234 5678 9012 3456 사용"))
                .isEqualTo("결제 카드 [CARD] 사용");
    }

    @Test
    void 여러PII가혼재할때전화번호와카드번호를마스킹하고이메일은유지한다() {
        String input = "고객 010-9876-5432, 이메일 user@test.co.kr, 카드 4321-8765-2109-6543";

        String result = preprocessor.process(input);

        assertThat(result).doesNotContain("010-9876-5432");
        assertThat(result).doesNotContain("4321-8765-2109-6543");
        assertThat(result).contains("[PHONE]");
        assertThat(result).contains("[CARD]");
        assertThat(result).contains("user@test.co.kr");
    }

    @Test
    void 최대길이초과시_최대길이에서절단한다() {
        String input = "a".repeat(200);

        assertThat(preprocessor.process(input)).hasSize(150);
    }

    @Test
    void 최대길이이하는절단하지않는다() {
        String input = "정상 길이 텍스트입니다.";

        assertThat(preprocessor.process(input)).isEqualTo(input);
    }

    @Test
    void 정확히최대길이는절단하지않는다() {
        String input = "a".repeat(150);

        assertThat(preprocessor.process(input)).hasSize(150);
    }

    @Test
    void 마스킹후최대길이초과시절단한다() {
        String prefix = "a".repeat(140);
        String input = prefix + " 010-1234-5678 추가텍스트";

        String result = preprocessor.process(input);

        assertThat(result).hasSize(150);
        assertThat(result).startsWith(prefix);
    }
}
