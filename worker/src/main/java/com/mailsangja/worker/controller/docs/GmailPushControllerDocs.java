package com.mailsangja.worker.controller.docs;

import com.mailsangja.worker.dto.gmail.push.GooglePubsubPushRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Gmail Push", description = "Gmail Pub/Sub Push Webhook API")
public interface GmailPushControllerDocs {

    @Operation(
            summary = "Gmail Push 웹훅 수신",
            description = "Google Pub/Sub에서 전달한 Gmail push 메시지를 수신하고 history 동기화를 수행합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "처리 성공",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Pub/Sub 메시지 형식이 잘못됨",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Pub/Sub Authorization 또는 OIDC 토큰 검증 실패",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "허용되지 않은 Pub/Sub OIDC 토큰",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "연결된 메일 계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<Void> handlePush(
            @Parameter(description = "Google Pub/Sub Push OIDC Bearer 토큰", required = true)
            String authorization,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Google Pub/Sub Push 요청 본문",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GooglePubsubPushRequest.class))
            )
            GooglePubsubPushRequest request
    );
}
