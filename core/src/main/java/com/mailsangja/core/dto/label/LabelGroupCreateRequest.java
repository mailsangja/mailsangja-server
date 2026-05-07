package com.mailsangja.core.dto.label;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Schema(description = "라벨 그룹 생성 요청")
public record LabelGroupCreateRequest(
        @Schema(description = "라벨 그룹 이름", example = "업무 묶음")
        @NotBlank
        String name,

        @Schema(description = "라벨 ID 목록")
        @NotEmpty
        List<@NotNull UUID> labelIds,

        @Schema(description = "표시 순서", example = "0")
        @PositiveOrZero
        int order
) {

    public LabelGroupCreateRequest {
        if (labelIds != null) {
            labelIds = Collections.unmodifiableList(new ArrayList<>(labelIds));
        }
    }
}
