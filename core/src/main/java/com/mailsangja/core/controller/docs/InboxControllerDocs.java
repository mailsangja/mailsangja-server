package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.inbox.ThreadDetailResponse;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Inbox", description = "통합 인박스 API")
public interface InboxControllerDocs {

    @Operation(
            summary = "받은 편지함 스레드 목록 조회",
            description = "로그인한 사용자의 모든 메일 계정에서 수신된 스레드 목록을 최신순으로 조회합니다. " +
                    "마커 기반 무한 스크롤 방식으로 동작합니다. " +
                    "첫 요청은 marker 없이 호출하고, 이후 응답의 nextMarker를 다음 요청의 marker로 전달합니다. " +
                    "nextMarker가 null이면 마지막 페이지입니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스레드 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 marker",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<MarkerSliceResponse<ThreadSummaryResponse>> getInbox(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "이전 응답의 nextMarker (첫 요청 시 생략)", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) UUID marker,
            @Parameter(description = "한 번에 조회할 스레드 수", example = "50")
            @RequestParam(defaultValue = "${mailsangja.inbox.page-size:50}") int size
    );

    @Operation(
            summary = "보낸 편지함 스레드 목록 조회",
            description = "로그인한 사용자의 모든 메일 계정에서 발신된 스레드 목록을 최신순으로 조회합니다. " +
                    "마커 기반 무한 스크롤 방식으로 동작합니다. " +
                    "첫 요청은 marker 없이 호출하고, 이후 응답의 nextMarker를 다음 요청의 marker로 전달합니다. " +
                    "nextMarker가 null이면 마지막 페이지입니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스레드 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 marker",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<MarkerSliceResponse<ThreadSummaryResponse>> getSent(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "이전 응답의 nextMarker (첫 요청 시 생략)", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) UUID marker,
            @Parameter(description = "한 번에 조회할 스레드 수", example = "50")
            @RequestParam(defaultValue = "${mailsangja.inbox.page-size:50}") int size
    );

    @Operation(
            summary = "메일 스레드 상세 조회",
            description = "특정 스레드의 전체 대화 (수신 + 발신 메시지)를 sentAt ASC 정렬로 조회합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "스레드 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "스레드 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "스레드를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<ThreadDetailResponse> getThreadDetail(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "스레드 내부 ID", required = true)
            @PathVariable UUID threadId
    );
}
