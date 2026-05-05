package com.mailsangja.db.entity.label;

import com.mailsangja.db.entity.common.BaseEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "label_group_labels")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LabelGroupLabel extends BaseEntity {

    @EmbeddedId
    private LabelGroupLabelId id;

    @MapsId("labelGroupId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_group_id", nullable = false)
    private LabelGroup labelGroup;

    @MapsId("labelId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    public static LabelGroupLabel of(LabelGroup labelGroup, Label label) {
        UUID labelGroupId = labelGroup.getId();
        return LabelGroupLabel.builder()
                .id(new LabelGroupLabelId(labelGroupId, label.getId()))
                .labelGroup(labelGroup)
                .label(label)
                .build();
    }

    public boolean hasLabelId(UUID labelId) {
        return label.getId().equals(labelId);
    }
}
