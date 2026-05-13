package com.mailsangja.core.dto.label;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.label.LabelGroup;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "라벨 그룹 응답")
public record LabelGroupResponse(
        @Schema(description = "라벨 그룹 ID", format = "uuid")
        UUID id,

        @Schema(description = "라벨 그룹 이름", example = "업무 묶음")
        String name,

        @Schema(description = "표시 순서", example = "0")
        int order,

        @Schema(description = "라벨 ID 목록")
        List<UUID> labelIds
) {

    public static LabelGroupResponse of(LabelGroup labelGroup, List<Label> labels) {
        return new LabelGroupResponse(
                labelGroup.getId(),
                labelGroup.getName(),
                labelGroup.getDisplayOrder(),
                labels.stream()
                        .map(Label::getId)
                        .toList()
        );
    }
}
