package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Star", description = "별표 API")
public interface StarControllerDocs {

    @Operation(
            summary = "별표 스레드 목록 조회",
            description = "로그인한 사용자의 모든 메일 계정에서 별표가 지정된 스레드 목록을 최신순으로 조회합니다. " +
                    "마커 기반 무한 스크롤 방식으로 동작합니다. " +
                    "첫 요청은 marker 없이 호출하고, 이후 응답의 nextMarker를 다음 요청의 marker로 전달합니다. " +
                    "nextMarker가 null이면 마지막 페이지입니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "별표 스레드 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<MarkerSliceResponse<ThreadSummaryResponse>> getStarred(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "이전 응답의 nextMarker (첫 요청 시 생략)", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) UUID marker,
            @Parameter(description = "한 번에 조회할 스레드 수", example = "50")
            @RequestParam(defaultValue = "${mailsangja.inbox.page-size:50}") int size,
            @Parameter(description = "라벨 ID 필터 (복수 지정 가능)")
            @RequestParam(required = false, name = "labelId") List<UUID> labelIds,
            @Parameter(description = "읽음 여부 필터 (true: 읽음만, false: 안읽음만, 생략: 전체)")
            @RequestParam(required = false) Boolean read,
            @Parameter(description = "검색어 (제목·본문·첨부파일명 대상 전문 검색)")
            @RequestParam(required = false) String q
    );

    @Operation(
            summary = "스레드 별표 토글",
            description = "특정 스레드의 별표 상태를 토글합니다. " +
                    "별표 ON 시 해당 gmailThreadId의 모든 메시지에 별표가 적용됩니다. " +
                    "별표 OFF 시 해당 gmailThreadId의 모든 메시지 별표가 해제됩니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "별표 토글 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "스레드 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "스레드를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> toggleThreadStar(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "스레드 내부 ID", required = true)
            @PathVariable UUID threadId
    );

    @Operation(
            summary = "메시지 별표 토글",
            description = "특정 메시지의 별표 상태를 토글합니다. " +
                    "메시지에 별표 ON 시 스레드도 별표 처리됩니다. " +
                    "메시지에 별표 OFF 시 해당 스레드에 별표 메시지가 남아 있지 않으면 스레드 별표도 해제됩니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "별표 토글 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "메시지 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> toggleMessageStar(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "메시지 내부 ID", required = true)
            @PathVariable UUID messageId
    );
}
