package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.trash.TrashThreadDetailResponse;
import com.mailsangja.core.dto.trash.TrashThreadSummaryResponse;
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

@Tag(name = "Trash", description = "메일 삭제 및 휴지통 API")
public interface TrashControllerDocs {

    @Operation(
            summary = "스레드 삭제 (휴지통 이동)",
            description = "스레드와 관련된 모든 메시지를 soft delete 처리하고, Gmail에서도 해당 스레드를 휴지통으로 이동합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "스레드 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "스레드를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "502", description = "Gmail API 호출 실패",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> deleteThread(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "스레드 내부 ID", required = true) @PathVariable UUID threadId
    );

    @Operation(
            summary = "메시지 삭제 (휴지통 이동)",
            description = "특정 메시지를 soft delete 처리하고, Gmail에서도 해당 메시지를 휴지통으로 이동합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "메시지 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "스레드 또는 메시지를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "502", description = "Gmail API 호출 실패",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> deleteMessage(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "메시지 내부 ID", required = true) @PathVariable UUID messageId
    );

    @Operation(
            summary = "휴지통 목록 조회",
            description = "로그인한 사용자의 모든 메일 계정에서 삭제된 메시지를 조회합니다. "
                    + "같은 gmailThreadId의 메시지는 하나의 항목으로 묶여 반환됩니다. "
                    + "labelId는 여러 번 전달할 수 있으며, 전달된 라벨 중 하나라도 부착된 항목을 조회합니다. "
                    + "read는 읽음 여부 필터이며 생략하면 전체를 조회합니다. "
                    + "페이지네이션은 메시지 기준(deletedAt DESC)으로 적용됩니다. "
                    + "q 파라미터가 있으면 FTS 모드로 동작하며 labelId, read 필터가 함께 적용됩니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "휴지통 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<MarkerSliceResponse<TrashThreadSummaryResponse>> getTrashThreads(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "마커 (이전 응답의 nextMarker — 메시지 ID)", required = false) @RequestParam(required = false) UUID marker,
            @Parameter(description = "페이지 크기", example = "50") @RequestParam(defaultValue = "${mailsangja.inbox.page-size:50}") int size,
            @Parameter(description = "필터링할 라벨 ID 목록. 여러 labelId 중 하나라도 부착되어 있으면 포함", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false, name = "labelId") List<UUID> labelIds,
            @Parameter(description = "읽음 여부 필터. 생략 시 전체 조회", example = "false")
            @RequestParam(required = false) Boolean read,
            @Parameter(description = "FTS 검색어. 입력 시 labelId·read 필터와 함께 적용됩니다. websearch_to_tsquery 문법 지원", example = "프로젝트 미팅")
            @RequestParam(required = false) String q
    );

    @Operation(
            summary = "휴지통 스레드 상세 조회",
            description = "특정 스레드(threadId)에 해당하는 모든 삭제된 메시지를 조회합니다. "
                    + "INBOUND/OUTBOUND 두 방향의 메시지를 모두 포함하며 sentAt 오름차순으로 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "스레드 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "스레드를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<TrashThreadDetailResponse> getTrashThreadDetail(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "스레드 내부 ID", required = true) @PathVariable UUID threadId
    );

    @Operation(
            summary = "스레드 복구",
            description = "휴지통에 있는 스레드와 관련된 모든 메시지를 restore 처리하고, Gmail에서도 해당 스레드를 untrash 합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "복구 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "스레드 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "스레드를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "스레드가 삭제된 상태가 아님",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "502", description = "Gmail API 호출 실패",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> restoreThread(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "스레드 내부 ID", required = true) @PathVariable UUID threadId
    );

    @Operation(
            summary = "메시지 복구",
            description = "휴지통에 있는 특정 메시지를 restore 처리하고, Gmail에서도 해당 메시지를 untrash 합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "복구 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "메시지 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "스레드 또는 메시지를 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "메시지가 삭제된 상태가 아님",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "502", description = "Gmail API 호출 실패",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> restoreMessage(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "메시지 내부 ID", required = true) @PathVariable UUID messageId
    );
}
