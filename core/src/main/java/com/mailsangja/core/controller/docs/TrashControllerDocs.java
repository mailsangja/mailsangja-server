package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
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
            summary = "휴지통 스레드 목록 조회",
            description = "로그인한 사용자의 모든 메일 계정에서 삭제(휴지통)된 스레드 목록을 조회합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "휴지통 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<List<TrashThreadSummaryResponse>> getTrashThreads(
            @Parameter(hidden = true) @AuthUser User user
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
