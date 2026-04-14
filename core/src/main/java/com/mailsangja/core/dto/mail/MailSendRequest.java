package com.mailsangja.core.dto.mail;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "메일 전송 요청")
public record MailSendRequest(
        @Schema(description = "메일 작성 컨텍스트를 식별하는 compose session ID", example = "8d3e8eb8-2c8a-41d5-beb9-5e9c12d85ddf")
        String composeSessionId,

        @Schema(description = "보내는 메일 주소", example = "sender@gmail.com")
        String from,

        @ArraySchema(
                arraySchema = @Schema(description = "수신 메일 주소 목록"),
                schema = @Schema(example = "user@example.com")
        )
        List<String> to,

        @ArraySchema(
                arraySchema = @Schema(description = "참조 메일 주소 목록"),
                schema = @Schema(example = "manager@example.com")
        )
        List<String> cc,

        @ArraySchema(
                arraySchema = @Schema(description = "숨은 참조 메일 주소 목록"),
                schema = @Schema(example = "audit@example.com")
        )
        List<String> bcc,

        @Schema(description = "메일 제목", example = "회의 자료 전달드립니다.")
        String subject,

        @Schema(description = "메일 본문", example = "안녕하세요.\n회의 자료 전달드립니다.")
        String content,

        @ArraySchema(
                arraySchema = @Schema(description = "첨부파일 목록"),
                schema = @Schema(type = "string", format = "binary")
        )
        List<MultipartFile> attachments
) {
}
