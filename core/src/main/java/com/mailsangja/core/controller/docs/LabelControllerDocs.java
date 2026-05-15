package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.label.LabelCreateRequest;
import com.mailsangja.core.dto.label.LabelDetailResponse;
import com.mailsangja.core.dto.label.LabelListResponse;
import com.mailsangja.core.dto.label.LabelRuleUpdateRequest;
import com.mailsangja.core.dto.label.LabelUpdateRequest;
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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "Label", description = "라벨 CRUD API")
public interface LabelControllerDocs {

    @Operation(
            summary = "라벨 목록 조회",
            description = "로그인한 사용자의 활성 라벨 목록을 displayOrder ASC 정렬로 반환합니다. 라벨별 읽지 않은 스레드 수를 포함합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<List<LabelListResponse>> getLabels(
            @Parameter(hidden = true) @AuthUser User user
    );

    @Operation(
            summary = "라벨 상세 조회",
            description = "특정 라벨의 메타 정보와 타입이 고정된 rule 객체를 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "라벨을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<LabelDetailResponse> getLabelDetail(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 ID", required = true) @PathVariable UUID labelId
    );

    @Operation(
            summary = "라벨 생성",
            description = "새 라벨을 생성합니다. rule이 존재하면 과거 메일 재분류 작업이 비동기로 발행됩니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "동일한 이름의 라벨이 이미 존재함",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<LabelDetailResponse> createLabel(
            @Parameter(hidden = true) @AuthUser User user,
            @Valid @RequestBody LabelCreateRequest request
    );

    @Operation(
            summary = "라벨 수정",
            description = "라벨의 이름, 색상, 알림 설정, 순서를 수정합니다. null 필드는 변경하지 않습니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "라벨을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "동일한 이름의 라벨이 이미 존재함",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<LabelDetailResponse> updateLabel(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 ID", required = true) @PathVariable UUID labelId,
            @RequestBody LabelUpdateRequest request
    );

    @Operation(
            summary = "라벨 규칙 수정",
            description = "라벨의 자동 분류 규칙(rule JSON)을 수정합니다. 수정 후 과거 메일 재분류 작업이 비동기로 발행됩니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 규칙 수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 규칙 JSON",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "라벨을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<LabelDetailResponse> updateLabelRule(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 ID", required = true) @PathVariable UUID labelId,
            @RequestBody LabelRuleUpdateRequest request
    );

    @Operation(
            summary = "라벨 삭제",
            description = "라벨을 soft delete 처리합니다. 삭제 시 해당 라벨의 Message/Thread 라벨 매핑을 즉시 제거합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "라벨 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "라벨을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> deleteLabel(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 ID", required = true) @PathVariable UUID labelId
    );

    @Operation(
            summary = "라벨 제안 생성",
            description = "환경변수(LABEL_SUGGESTIONS_JSON)에서 라벨 제안 목록을 읽어 DB에 confirmed=false 상태로 저장합니다. 이미 제안 상태(confirmed=false)로 동일한 이름의 라벨이 존재하면 중복 저장하지 않습니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 제안 생성 성공 (새로 저장된 제안 목록만 반환)"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "제안 JSON 읽기 실패",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<List<LabelListResponse>> createSuggestions(
            @Parameter(hidden = true) @AuthUser User user
    );

    @Operation(
            summary = "라벨 제안 목록 조회",
            description = "현재 사용자의 미승인(confirmed=false) 라벨 제안 목록을 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 제안 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<List<LabelListResponse>> getSuggestions(
            @Parameter(hidden = true) @AuthUser User user
    );

    @Operation(
            summary = "라벨 제안 단건 승인",
            description = "라벨 제안을 수정하여 confirmed=true로 전환합니다. 요청 바디로 이름·색상·순서·알림정책·규칙을 원하는 값으로 변경한 뒤 승인할 수 있습니다. rule이 있는 라벨은 승인 후 과거 메일 재분류 작업이 비동기로 발행됩니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 제안 승인 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "라벨 제안을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "동일한 이름의 라벨이 이미 존재함",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<LabelDetailResponse> approveSuggestion(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 제안 ID", required = true) @PathVariable UUID suggestionId,
            @Valid @RequestBody LabelCreateRequest request
    );

    @Operation(
            summary = "라벨 제안 삭제",
            description = "라벨 제안을 soft delete 처리합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "라벨 제안 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "라벨 제안을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> deleteSuggestion(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 제안 ID", required = true) @PathVariable UUID suggestionId
    );
}
