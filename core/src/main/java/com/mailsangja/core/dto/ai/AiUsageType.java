package com.mailsangja.core.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 기능 타입")
public enum AiUsageType {

    @Schema(description = "AI 메일 작성")
    MAIL_DRAFT,

    @Schema(description = "AI 메일 검토")
    MAIL_REVIEW,

    @Schema(description = "AI 라벨 추천")
    LABEL_SUGGESTION
}
