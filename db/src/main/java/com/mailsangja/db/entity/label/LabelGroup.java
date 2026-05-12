package com.mailsangja.db.entity.label;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "label_groups")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LabelGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder.Default
    @OneToMany(mappedBy = "labelGroup", cascade = CascadeType.ALL)
    private List<LabelGroupLabel> labelGroupLabels = new ArrayList<>();

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void replaceLabels(List<Label> labels) {
        Map<UUID, Label> requestedLabels = labels.stream()
                .collect(Collectors.toMap(
                        Label::getId,
                        label -> label,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        Map<UUID, LabelGroupLabel> existingMappings = labelGroupLabels.stream()
                .collect(Collectors.toMap(mapping -> mapping.getLabel().getId(), mapping -> mapping));

        labelGroupLabels.forEach(mapping -> updateMappingState(mapping, requestedLabels));
        requestedLabels.forEach((labelId, label) -> {
            if (!existingMappings.containsKey(labelId)) {
                labelGroupLabels.add(LabelGroupLabel.of(this, label));
            }
        });
    }

    public List<Label> getActiveLabels() {
        return labelGroupLabels.stream()
                .filter(mapping -> !mapping.isDeleted())
                .map(LabelGroupLabel::getLabel)
                .filter(label -> !label.isDeleted())
                .sorted(Comparator.comparing(Label::getDisplayOrder)
                        .thenComparing(Label::getCreatedAt))
                .toList();
    }

    public void deleteWithLabels() {
        labelGroupLabels.stream()
                .filter(mapping -> !mapping.isDeleted())
                .forEach(LabelGroupLabel::delete);
        delete();
    }

    public void deleteLabelMapping(UUID labelId) {
        labelGroupLabels.stream()
                .filter(mapping -> !mapping.isDeleted())
                .filter(mapping -> mapping.hasLabelId(labelId))
                .forEach(LabelGroupLabel::delete);
        if (getActiveLabels().isEmpty()) {
            delete();
        }
    }

    private void updateMappingState(LabelGroupLabel mapping, Map<UUID, Label> requestedLabels) {
        UUID labelId = mapping.getLabel().getId();
        if (requestedLabels.containsKey(labelId)) {
            mapping.restore();
            return;
        }
        if (!mapping.isDeleted()) {
            mapping.delete();
        }
    }
}
