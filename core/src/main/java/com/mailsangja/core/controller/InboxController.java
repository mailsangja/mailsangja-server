package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.InboxControllerDocs;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.inbox.ThreadDetailResponse;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.core.facade.InboxFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InboxController implements InboxControllerDocs {

    private final InboxFacade inboxFacade;

    @Override
    @GetMapping("/api/v1/threads/inbox")
    public ResponseEntity<MarkerSliceResponse<ThreadSummaryResponse>> getInbox(
            @AuthUser User user,
            @RequestParam(required = false) UUID marker,
            @RequestParam(defaultValue = "${mailsangja.inbox.page-size:50}") int size
    ) {
        return ResponseEntity.ok(inboxFacade.getInbox(user, marker, size));
    }

    @Override
    @GetMapping("/api/v1/threads/sent")
    public ResponseEntity<MarkerSliceResponse<ThreadSummaryResponse>> getSent(
            @AuthUser User user,
            @RequestParam(required = false) UUID marker,
            @RequestParam(defaultValue = "${mailsangja.inbox.page-size:50}") int size
    ) {
        return ResponseEntity.ok(inboxFacade.getSent(user, marker, size));
    }

    @Override
    @GetMapping("/api/v1/threads/{threadId}")
    public ResponseEntity<ThreadDetailResponse> getThreadDetail(
            @AuthUser User user,
            @PathVariable UUID threadId
    ) {
        return ResponseEntity.ok(inboxFacade.getThreadDetail(user, threadId));
    }
}
