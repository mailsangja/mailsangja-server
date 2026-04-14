package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.mail.MailComposeResponse;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Mail", description = "메일 작성 및 전송 API")
public interface MailControllerDocs {

    @Operation(
            summary = "메일 전송",
            description = "사용자가 작성한 메일을 전송하기 위한 API입니다. multipart/form-data 형식으로 본문 필드와 첨부파일을 함께 전달합니다. " +
                    "composeSessionId를 함께 전달해 작성 컨텍스트를 식별합니다. " +
                    "현재는 Controller/docs 스캐폴드만 구현되어 있으며 실제 발송 로직은 아직 연결되지 않았습니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "메일 전송 요청 수락 성공 (현재는 실제 발송 로직 미구현)",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<Void> sendMail(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody(
                    description = "메일 전송 요청 정보",
                    required = true,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = MailSendRequest.class)
                    )
            )
            MailSendRequest request
    );

    @Operation(
            summary = "메일 작성 세션 발급",
            description = "사용자가 메일 작성하기를 시작할 때 업로드/작성 컨텍스트 식별용 composeSessionId를 발급합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "composeSessionId 발급 성공",
                    content = @Content(schema = @Schema(implementation = MailComposeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<MailComposeResponse> createCompose(
            @Parameter(hidden = true) @AuthUser User user
    );
}
