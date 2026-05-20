package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.AiControllerDocs;
import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.core.facade.AiFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiController implements AiControllerDocs {

    private final AiFacade aiFacade;

    @Override
    @GetMapping("/api/v1/ai/models")
    public ResponseEntity<AiModelListResponse> getModels(@AuthUser User user) {
        return ResponseEntity.ok(aiFacade.getModels());
    }
}
