package com.mailsangja.db.common.label;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "라벨 자동 분류 규칙 (그룹 간 OR, 그룹 내 조건 간 AND)")
public record LabelRule(
        @Schema(description = "조건 그룹 목록 (그룹 간 OR 조건)")
        List<Group> groups
) {

    @Schema(description = "조건 그룹 (그룹 내 조건 간 AND 조건)")
    public record Group(
            @Schema(description = "단일 조건 목록")
            List<Condition> conditions
    ) {}

    @Schema(description = "단일 조건")
    public record Condition(
            @Schema(description = "조건 대상 필드")
            Field field,

            @Schema(description = "비교 연산자")
            Operator operator,

            @Schema(description = "비교 값 (HAS_ATTACHMENT 필드는 'true'/'false'로 입력)", example = "example@gmail.com")
            String value
    ) {}

    @Schema(description = "조건 대상 필드")
    public enum Field {
        @Schema(description = "발신 메일 계정 ID") MAIL_ACCOUNT,
        @Schema(description = "발신자 이메일 주소") FROM_ADDRESS,
        @Schema(description = "발신자 도메인") FROM_DOMAIN,
        @Schema(description = "수신자 이메일 주소") TO_ADDRESS,
        @Schema(description = "참조 이메일 주소") CC_ADDRESS,
        @Schema(description = "메일 제목") SUBJECT,
        @Schema(description = "메일 본문 텍스트") BODY_TEXT,
        @Schema(description = "첨부파일 존재 여부") HAS_ATTACHMENT
    }

    @Schema(description = "비교 연산자")
    public enum Operator {
        @Schema(description = "정확히 일치") EQUALS,
        @Schema(description = "포함") CONTAINS,
        @Schema(description = "포함하지 않음") NOT_CONTAINS,
        @Schema(description = "불리언 비교 (HAS_ATTACHMENT 전용)") BOOLEAN
    }
}
