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

import java.util.UUID;

@Tag(name = "Mail", description = "메일 작성 및 전송 API")
public interface MailControllerDocs {

    @Operation(
            summary = "메일 전송",
            description = "사용자가 작성한 메일을 전송하기 위한 API입니다. multipart/form-data 형식으로 일반 form 필드와 첨부파일을 함께 전달합니다. " +
                    "20MB 이하의 일반 첨부파일만 허용합니다. " +
                    "from/to/cc/bcc는 `user@example.com` 또는 `\"이름\" <user@example.com>` 형식을 지원합니다. " +
                    "to/cc/bcc/attachments 배열 필드는 동일한 필드를 여러 번 반복 전달합니다.",
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

    @Operation(
            summary = "메일 첨부파일 다운로드",
            description = "attachmentId에 해당하는 첨부파일을 다운로드합니다. " +
                    "로그인 사용자의 메일 계정에 속한 첨부파일만 접근할 수 있으며, 현재는 Gmail 첨부파일만 지원합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "첨부파일 다운로드 성공",
                    content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "첨부파일 접근 권한 없음",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "첨부파일을 찾을 수 없음",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "501",
                    description = "현재 지원하지 않는 메일 제공자의 첨부파일",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "원본 첨부파일 조회 실패",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<byte[]> getAttachment(
            @Parameter(hidden = true) @AuthUser User user,
            @Parameter(description = "첨부파일 ID", required = true)
            UUID attachmentId
    );
}
