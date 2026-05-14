package com.mailsangja.core.dto.label;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "라벨 제안 승인 요청")
public record LabelSuggestionApproveRequest(
        @NotEmpty
        @Schema(description = "승인할 라벨 제안 ID 목록")
        List<UUID> ids
) {
}
