package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.StarControllerDocs;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.core.facade.StarFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StarController implements StarControllerDocs {

    private final StarFacade starFacade;

    @Override
    @GetMapping("/api/v1/threads/starred")
    public ResponseEntity<MarkerSliceResponse<ThreadSummaryResponse>> getStarred(
            @AuthUser User user,
            @RequestParam(required = false) UUID marker,
            @RequestParam(defaultValue = "${mailsangja.inbox.page-size:50}") int size,
            @RequestParam(required = false, name = "labelId") List<UUID> labelIds,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(starFacade.getStarred(user, marker, size, labelIds, read, q));
    }

    @Override
    @PostMapping("/api/v1/threads/{threadId}/star")
    public ResponseEntity<Void> toggleThreadStar(
            @AuthUser User user,
            @PathVariable UUID threadId
    ) {
        starFacade.toggleThreadStar(user, threadId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/api/v1/messages/{messageId}/star")
    public ResponseEntity<Void> toggleMessageStar(
            @AuthUser User user,
            @PathVariable UUID messageId
    ) {
        starFacade.toggleMessageStar(user, messageId);
        return ResponseEntity.ok().build();
    }
}
