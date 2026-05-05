package com.mailsangja.core.dto.label;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "라벨 그룹 생성 요청")
public record LabelGroupCreateRequest(
        @Schema(description = "라벨 그룹 이름", example = "업무 묶음")
        String name,

        @Schema(description = "라벨 ID 목록")
        List<UUID> labelIds,

        @Schema(description = "표시 순서", example = "0")
        int order
) {

    public LabelGroupCreateRequest {
        if (name == null || name.isBlank()) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_NAME_BLANK);
        }
        if (labelIds == null || labelIds.isEmpty()) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_LABELS_EMPTY);
        }
        if (labelIds.stream().anyMatch(id -> id == null)) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_LABELS_INVALID);
        }
        labelIds = List.copyOf(labelIds);
    }
}
