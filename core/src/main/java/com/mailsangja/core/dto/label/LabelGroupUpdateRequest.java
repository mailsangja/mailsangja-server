package com.mailsangja.core.dto.label;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "라벨 그룹 수정 요청 (null 필드는 변경하지 않음)")
public record LabelGroupUpdateRequest(
        @Schema(description = "라벨 그룹 이름", example = "중요 업무")
        String name,

        @Schema(description = "라벨 ID 목록")
        List<UUID> labelIds,

        @Schema(description = "표시 순서", example = "1")
        Integer order
) {

    public LabelGroupUpdateRequest {
        if (name != null && name.isBlank()) {
            throw new LabelException(LabelErrorCode.LABEL_GROUP_NAME_BLANK);
        }
        if (labelIds != null) {
            if (labelIds.isEmpty()) {
                throw new LabelException(LabelErrorCode.LABEL_GROUP_LABELS_EMPTY);
            }
            if (labelIds.stream().anyMatch(id -> id == null)) {
                throw new LabelException(LabelErrorCode.LABEL_GROUP_LABELS_INVALID);
            }
            labelIds = List.copyOf(labelIds);
        }
    }
}
