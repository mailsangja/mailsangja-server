package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
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
                    "20MB 이하의 일반 첨부파일만 허용합니다. " +
                    "to/cc/bcc/attachments 배열 필드는 JSON 배열 문자열이 아니라 동일한 필드를 여러 번 반복해 전달해야 합니다. " +
                    "예: to=user1@example.com, to=user2@example.com",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "메일 전송 성공",
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
                            schema = @Schema(implementation = MailSendRequest.class),
                            encoding = {
                                    @Encoding(name = "to"),
                                    @Encoding(name = "cc"),
                                    @Encoding(name = "bcc"),
                                    @Encoding(name = "attachments")
                            }
                    )
            )
            MailSendRequest request
    );
}
