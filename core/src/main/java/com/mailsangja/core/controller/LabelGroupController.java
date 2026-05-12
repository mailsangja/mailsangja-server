package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.LabelGroupControllerDocs;
import com.mailsangja.core.dto.label.LabelGroupCreateRequest;
import com.mailsangja.core.dto.label.LabelGroupResponse;
import com.mailsangja.core.dto.label.LabelGroupUpdateRequest;
import com.mailsangja.core.facade.LabelGroupFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LabelGroupController implements LabelGroupControllerDocs {

    private final LabelGroupFacade labelGroupFacade;

    @Override
    @GetMapping("/api/v1/label-groups")
    public ResponseEntity<List<LabelGroupResponse>> getLabelGroups(@AuthUser User user) {
        return ResponseEntity.ok(labelGroupFacade.getLabelGroups(user));
    }

    @Override
    @GetMapping("/api/v1/label-groups/{labelGroupId}")
    public ResponseEntity<LabelGroupResponse> getLabelGroupDetail(
            @AuthUser User user,
            @PathVariable UUID labelGroupId
    ) {
        return ResponseEntity.ok(labelGroupFacade.getLabelGroupDetail(user, labelGroupId));
    }

    @Override
    @PostMapping("/api/v1/label-groups")
    public ResponseEntity<LabelGroupResponse> createLabelGroup(
            @AuthUser User user,
            @RequestBody LabelGroupCreateRequest request
    ) {
        return ResponseEntity.ok(labelGroupFacade.createLabelGroup(user, request));
    }

    @Override
    @PatchMapping("/api/v1/label-groups/{labelGroupId}")
    public ResponseEntity<LabelGroupResponse> updateLabelGroup(
            @AuthUser User user,
            @PathVariable UUID labelGroupId,
            @RequestBody LabelGroupUpdateRequest request
    ) {
        return ResponseEntity.ok(labelGroupFacade.updateLabelGroup(user, labelGroupId, request));
    }

    @Override
    @DeleteMapping("/api/v1/label-groups/{labelGroupId}")
    public ResponseEntity<Void> deleteLabelGroup(
            @AuthUser User user,
            @PathVariable UUID labelGroupId
    ) {
        labelGroupFacade.deleteLabelGroup(user, labelGroupId);
        return ResponseEntity.noContent().build();
    }
}
