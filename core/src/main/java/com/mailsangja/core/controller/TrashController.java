package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.TrashControllerDocs;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.trash.TrashThreadDetailResponse;
import com.mailsangja.core.dto.trash.TrashThreadSummaryResponse;
import com.mailsangja.core.facade.TrashFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TrashController implements TrashControllerDocs {

    private final TrashFacade trashFacade;

    @Override
    @DeleteMapping("/api/v1/threads/{threadId}")
    public ResponseEntity<Void> deleteThread(
            @AuthUser User user,
            @PathVariable UUID threadId
    ) {
        trashFacade.deleteThread(user, threadId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/api/v1/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthUser User user,
            @PathVariable UUID messageId
    ) {
        trashFacade.deleteMessage(user, messageId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/api/v1/trash/threads")
    public ResponseEntity<MarkerSliceResponse<TrashThreadSummaryResponse>> getTrashThreads(
            @AuthUser User user,
            @RequestParam(required = false) UUID marker,
            @RequestParam(defaultValue = "${mailsangja.inbox.page-size:50}") int size,
            @RequestParam(required = false, name = "labelId") List<UUID> labelIds,
            @RequestParam(required = false) Boolean read
    ) {
        return ResponseEntity.ok(trashFacade.getTrashThreads(user, marker, size, labelIds, read));
    }

    @Override
    @GetMapping("/api/v1/trash/threads/{threadId}")
    public ResponseEntity<TrashThreadDetailResponse> getTrashThreadDetail(
            @AuthUser User user,
            @PathVariable UUID threadId
    ) {
        return ResponseEntity.ok(trashFacade.getTrashThreadDetail(user, threadId));
    }

    @Override
    @PostMapping("/api/v1/trash/threads/{threadId}/restore")
    public ResponseEntity<Void> restoreThread(
            @AuthUser User user,
            @PathVariable UUID threadId
    ) {
        trashFacade.restoreThread(user, threadId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/api/v1/trash/messages/{messageId}/restore")
    public ResponseEntity<Void> restoreMessage(
            @AuthUser User user,
            @PathVariable UUID messageId
    ) {
        trashFacade.restoreMessage(user, messageId);
        return ResponseEntity.noContent().build();
    }
}
