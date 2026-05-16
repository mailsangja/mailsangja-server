package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.mail.MailDraftStreamRequest;
import com.mailsangja.core.dto.mail.MailReviewRequest;
import com.mailsangja.core.dto.mail.MailReviewResponse;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Tag(name = "Mail", description = "메일 작성 및 전송 API")
public interface MailControllerDocs {

    @Operation(
            summary = "메일 전송",
            description = "사용자가 작성한 메일을 전송하기 위한 API입니다. multipart/form-data 형식으로 일반 form 필드와 첨부파일을 함께 전달합니다. " +
                    "20MB 이하의 일반 첨부파일과 본문 인라인 이미지를 허용합니다. " +
                    "from/to/cc/bcc는 `user@example.com` 또는 `\"이름\" <user@example.com>` 형식을 지원합니다. " +
                    "본문 인라인 이미지는 content의 `<img src=\"cid:{cid}\">`, inlineImages 파일, inlineImageCids 값을 같은 순서로 전달합니다. " +
                    "to/cc/bcc/attachments/inlineImages/inlineImageCids 배열 필드는 동일한 필드를 여러 번 반복 전달합니다.",
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
            @Parameter(description = "답장 대상 메시지 ID. 없으면 일반 전송으로 처리합니다.", required = false)
            UUID messageId,
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
                                    @Encoding(name = "attachments"),
                                    @Encoding(name = "inlineImages"),
                                    @Encoding(name = "inlineImageCids")
                            }
                    )
            )
            MailSendRequest request
    );

    @Operation(
            summary = "AI 메일 초안 스트리밍",
            description = "사용자의 요청과 메일 컨텍스트를 기반으로 AI 메일 초안을 SSE로 스트리밍합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    ResponseEntity<SseEmitter> streamDraft(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody(description = "AI 메일 초안 스트리밍 요청", required = true)
            MailDraftStreamRequest request
    );

    @Operation(
            summary = "AI 메일 전송 전 검토",
            description = "메일 제목과 본문을 segment 단위로 나누어 AI가 맞춤법, 띄어쓰기, 문맥, 톤 문제를 검토하고 적용 가능한 수정 후보와 위치 정보를 반환합니다.",
            security = @SecurityRequirement(name = "cookieAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "메일 검토 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "메일 검토 요청 형식 오류",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "월간 AI 사용 한도 초과",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "AI 응답 형식 오류",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 모델 사용 불가",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    ResponseEntity<MailReviewResponse> reviewMail(
            @Parameter(hidden = true) @AuthUser User user,
            @RequestBody(description = "AI 메일 전송 전 검토 요청", required = true)
            MailReviewRequest request
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
