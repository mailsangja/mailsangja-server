package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.ai.AiModelListResponse;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "AI", description = "AI 기능 API")
public interface AiControllerDocs {

    @Operation(
            summary = "AI 모델 목록 조회",
            description = "AI 기능에서 선택할 수 있는 OpenRouter 모델 ID 목록과 기본 모델을 조회합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    ResponseEntity<AiModelListResponse> getModels(
            @Parameter(hidden = true) @AuthUser User user
    );
}
