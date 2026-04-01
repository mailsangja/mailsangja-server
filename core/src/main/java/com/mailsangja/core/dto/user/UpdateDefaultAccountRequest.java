package com.mailsangja.core.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "기본 발신 메일 계정 수정 요청")
public record UpdateDefaultAccountRequest(
        @Schema(description = "기본으로 설정할 메일 계정 ID")
        UUID mailAccountId
) {}
