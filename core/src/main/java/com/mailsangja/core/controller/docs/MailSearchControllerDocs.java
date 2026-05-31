package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.search.HybridMailSearchResponse;
import com.mailsangja.core.dto.search.HybridMailSearchScope;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Mail Search", description = "메일 검색 API")
public interface MailSearchControllerDocs {

    @Operation(
            summary = "하이브리드 메일 검색",
            description = "VectorStore 유사도 검색과 PostgreSQL FTS 검색 결과를 RRF로 병합해 message 단위 결과를 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "하이브리드 메일 검색 성공"),
            @ApiResponse(responseCode = "400", description = "검색 요청 형식 오류",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<HybridMailSearchResponse> searchHybrid(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "검색어", required = true, example = "프로젝트 일정 조율")
            @RequestParam String q,
            @Parameter(description = "검색 범위", example = "ALL")
            @RequestParam(required = false) HybridMailSearchScope scope,
            @Parameter(description = "특정 메일 계정 ID")
            @RequestParam(required = false) UUID mailAccountId,
            @Parameter(description = "필터링할 라벨 ID 목록. 여러 labelId 중 하나라도 부착되어 있으면 포함")
            @RequestParam(required = false, name = "labelId") List<UUID> labelIds,
            @Parameter(description = "읽음 여부 필터. 생략 시 전체 조회", example = "false")
            @RequestParam(required = false) Boolean read,
            @Parameter(description = "반환할 결과 수. 최대 50", example = "20")
            @RequestParam(required = false) Integer size
    );
}
