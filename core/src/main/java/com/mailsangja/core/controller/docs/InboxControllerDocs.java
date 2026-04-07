package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.common.SliceResponse;
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
                    "대화 기록 중 내가 마지막으로 답장한 경우에도 가장 최근 수신 메시지 기준으로 표시됩니다. " +
                    "무한 스크롤 방식으로 page를 순차 증가시켜 다음 항목을 요청합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "스레드 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = SliceResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<SliceResponse<ThreadSummaryResponse>> getInbox(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "페이지 번호 (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page
    );

    @Operation(
            summary = "보낸 편지함 스레드 목록 조회",
            description = "로그인한 사용자의 모든 메일 계정에서 발신된 스레드 목록을 최신순으로 조회합니다. " +
                    "무한 스크롤 방식으로 page를 순차 증가시켜 다음 항목을 요청합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "스레드 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = SliceResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<SliceResponse<ThreadSummaryResponse>> getSent(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "페이지 번호 (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page
    );

    @Operation(
            summary = "메일 스레드 상세 조회",
            description = "특정 스레드의 전체 대화 (수신 + 발신 메시지)를 sentAt ASC 정렬로 조회합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "스레드 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ThreadDetailResponse.class))
            ),
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
