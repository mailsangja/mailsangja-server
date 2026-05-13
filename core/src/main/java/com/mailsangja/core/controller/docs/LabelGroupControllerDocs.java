package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.label.LabelGroupCreateRequest;
import com.mailsangja.core.dto.label.LabelGroupResponse;
import com.mailsangja.core.dto.label.LabelGroupUpdateRequest;
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
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "Label Group", description = "라벨 그룹 CRUD API")
public interface LabelGroupControllerDocs {

    @Operation(
            summary = "라벨 그룹 목록 조회",
            description = "로그인한 사용자의 활성 라벨 그룹 목록을 displayOrder ASC 정렬로 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponse(responseCode = "200", description = "라벨 그룹 목록 조회 성공")
    ResponseEntity<List<LabelGroupResponse>> getLabelGroups(
            @Parameter(hidden = true) @AuthUser User user
    );

    @Operation(
            summary = "라벨 그룹 상세 조회",
            description = "특정 라벨 그룹과 포함된 라벨 목록을 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 그룹 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "라벨 그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<LabelGroupResponse> getLabelGroupDetail(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 그룹 ID", required = true) @PathVariable UUID labelGroupId
    );

    @Operation(
            summary = "라벨 그룹 생성",
            description = "라벨 그룹 이름, 포함할 라벨 목록, 표시 순서를 지정해 라벨 그룹을 생성합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 그룹 생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "동일한 이름의 라벨 그룹이 이미 존재함",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<LabelGroupResponse> createLabelGroup(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody LabelGroupCreateRequest request
    );

    @Operation(
            summary = "라벨 그룹 수정",
            description = "라벨 그룹 이름, 포함 라벨 목록, 표시 순서를 수정합니다. null 필드는 변경하지 않습니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라벨 그룹 수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "라벨 그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "동일한 이름의 라벨 그룹이 이미 존재함",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<LabelGroupResponse> updateLabelGroup(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 그룹 ID", required = true) @PathVariable UUID labelGroupId,
            @RequestBody LabelGroupUpdateRequest request
    );

    @Operation(
            summary = "라벨 그룹 삭제",
            description = "라벨 그룹과 그룹-라벨 연결만 soft delete 처리합니다. 라벨 및 메일 분류 결과에는 영향을 주지 않습니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "라벨 그룹 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "라벨 그룹을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<Void> deleteLabelGroup(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "라벨 그룹 ID", required = true) @PathVariable UUID labelGroupId
    );
}
