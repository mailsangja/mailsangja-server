package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.MailSearchControllerDocs;
import com.mailsangja.core.dto.search.HybridMailSearchResponse;
import com.mailsangja.core.dto.search.HybridMailSearchScope;
import com.mailsangja.core.facade.HybridMailSearchFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MailSearchController implements MailSearchControllerDocs {

    private final HybridMailSearchFacade hybridMailSearchFacade;

    @Override
    @GetMapping("/api/v1/mail/search/hybrid")
    public ResponseEntity<HybridMailSearchResponse> searchHybrid(
            @AuthUser User user,
            @RequestParam String q,
            @RequestParam(required = false) HybridMailSearchScope scope,
            @RequestParam(required = false) UUID mailAccountId,
            @RequestParam(required = false, name = "labelId") List<UUID> labelIds,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(hybridMailSearchFacade.search(user, q, scope, mailAccountId, labelIds, read, size));
    }
}
