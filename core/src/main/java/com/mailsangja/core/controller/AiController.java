package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthAdmin;
import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.AiControllerDocs;
import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.core.dto.ai.AiPlaygroundChatRequest;
import com.mailsangja.core.dto.ai.AiPlaygroundChatResponse;
import com.mailsangja.core.dto.ai.AiUsageListResponse;
import com.mailsangja.core.dto.ai.AiUsageType;
import com.mailsangja.core.facade.AiFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AiController implements AiControllerDocs {

    private final AiFacade aiFacade;

    @Override
    @PostMapping("/api/v1/ai/playground/chat")
    public ResponseEntity<AiPlaygroundChatResponse> chat(
            @AuthAdmin User user,
            @RequestBody AiPlaygroundChatRequest request
    ) {
        return ResponseEntity.ok(aiFacade.chat(user, request));
    }

    @Override
    @GetMapping("/api/v1/ai/models")
    public ResponseEntity<AiModelListResponse> getModels(@AuthUser User user) {
        return ResponseEntity.ok(aiFacade.getModels());
    }

    @Override
    @GetMapping("/api/v1/ai/usages")
    public ResponseEntity<AiUsageListResponse> getUsages(
            @AuthUser User user,
            @RequestParam(required = false) List<AiUsageType> type
    ) {
        return ResponseEntity.ok(aiFacade.getUsages(user, type));
    }
}
