package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.contact.ContactCreateRequest;
import com.mailsangja.core.dto.contact.ContactResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Contact", description = "주소록 API")
public interface ContactControllerDocs {

    @Operation(summary = "주소록 생성", description = "로그인한 사용자의 주소록에 연락처를 생성합니다.",
            security = @SecurityRequirement(name = "cookieAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주소록 생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "동일한 이메일의 연락처가 이미 존재함",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<ContactResponse> createContact(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody ContactCreateRequest request
    );

    @Operation(summary = "주소록 목록 조회", description = "로그인한 사용자의 활성 연락처 목록을 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주소록 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true)))
    })
    ResponseEntity<List<ContactResponse>> getContacts(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "이름 또는 이메일 검색어") @RequestParam(required = false) String keyword
    );
}
