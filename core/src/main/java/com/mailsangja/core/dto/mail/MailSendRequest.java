package com.mailsangja.core.dto.mail;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "메일 전송 요청")
public record MailSendRequest(
        @Schema(description = "보내는 메일 주소", example = "sender@gmail.com")
        String from,

        @ArraySchema(
                arraySchema = @Schema(description = "수신 메일 주소 목록. multipart/form-data 에서는 to 필드를 반복 전달합니다."),
                schema = @Schema(example = "user@example.com")
        )
        List<String> to,

        @ArraySchema(
                arraySchema = @Schema(description = "참조 메일 주소 목록. multipart/form-data 에서는 cc 필드를 반복 전달합니다."),
                schema = @Schema(example = "manager@example.com")
        )
        List<String> cc,

        @ArraySchema(
                arraySchema = @Schema(description = "숨은 참조 메일 주소 목록. multipart/form-data 에서는 bcc 필드를 반복 전달합니다."),
                schema = @Schema(example = "audit@example.com")
        )
        List<String> bcc,

        @Schema(description = "메일 제목", example = "회의 자료 전달드립니다.")
        String subject,

        @Schema(description = "메일 본문", example = "안녕하세요.\n회의 자료 전달드립니다.")
        String content,

        @ArraySchema(
                arraySchema = @Schema(description = "첨부파일 목록. multipart/form-data 에서는 attachments 필드를 반복 전달합니다."),
                schema = @Schema(type = "string", format = "binary")
        )
        List<MultipartFile> attachments
) {
}
