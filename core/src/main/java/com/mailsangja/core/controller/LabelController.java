package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.LabelControllerDocs;
import com.mailsangja.core.dto.label.LabelCreateRequest;
import com.mailsangja.core.dto.label.LabelDetailResponse;
import com.mailsangja.core.dto.label.LabelListResponse;
import com.mailsangja.core.dto.label.LabelRuleUpdateRequest;
import com.mailsangja.core.dto.label.LabelUpdateRequest;
import com.mailsangja.core.facade.LabelFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
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
public class LabelController implements LabelControllerDocs {

    private final LabelFacade labelFacade;

    @Override
    @GetMapping("/api/v1/labels")
    public ResponseEntity<List<LabelListResponse>> getLabels(@AuthUser User user) {
        return ResponseEntity.ok(labelFacade.getLabels(user));
    }

    @Override
    @GetMapping("/api/v1/labels/{labelId}")
    public ResponseEntity<LabelDetailResponse> getLabelDetail(
            @AuthUser User user,
            @PathVariable UUID labelId
    ) {
        return ResponseEntity.ok(labelFacade.getLabelDetail(user, labelId));
    }

    @Override
    @PostMapping("/api/v1/labels")
    public ResponseEntity<LabelDetailResponse> createLabel(
            @AuthUser User user,
            @Valid @RequestBody LabelCreateRequest request
    ) {
        return ResponseEntity.ok(labelFacade.createLabel(user, request));
    }

    @Override
    @PatchMapping("/api/v1/labels/{labelId}")
    public ResponseEntity<LabelDetailResponse> updateLabel(
            @AuthUser User user,
            @PathVariable UUID labelId,
            @RequestBody LabelUpdateRequest request
    ) {
        return ResponseEntity.ok(labelFacade.updateLabel(user, labelId, request));
    }

    @Override
    @PatchMapping("/api/v1/labels/{labelId}/rule")
    public ResponseEntity<LabelDetailResponse> updateLabelRule(
            @AuthUser User user,
            @PathVariable UUID labelId,
            @RequestBody LabelRuleUpdateRequest request
    ) {
        return ResponseEntity.ok(labelFacade.updateLabelRule(user, labelId, request));
    }

    @Override
    @DeleteMapping("/api/v1/labels/{labelId}")
    public ResponseEntity<Void> deleteLabel(
            @AuthUser User user,
            @PathVariable UUID labelId
    ) {
        labelFacade.deleteLabel(user, labelId);
        return ResponseEntity.noContent().build();
    }
}
