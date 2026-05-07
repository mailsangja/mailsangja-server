package com.mailsangja.core.dto.label;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Schema(description = "라벨 그룹 수정 요청 (null 필드는 변경하지 않음)")
public record LabelGroupUpdateRequest(
        @Schema(description = "라벨 그룹 이름", example = "중요 업무")
        @Pattern(regexp = ".*\\S.*", message = "라벨 그룹 이름은 공백일 수 없습니다.")
        String name,

        @Schema(description = "라벨 ID 목록")
        @Size(min = 1, message = "라벨 ID 목록은 비어 있을 수 없습니다.")
        List<@NotNull UUID> labelIds,

        @Schema(description = "표시 순서", example = "1")
        Integer order
) {

    public LabelGroupUpdateRequest {
        if (labelIds != null) {
            labelIds = Collections.unmodifiableList(new ArrayList<>(labelIds));
        }
    }
}
