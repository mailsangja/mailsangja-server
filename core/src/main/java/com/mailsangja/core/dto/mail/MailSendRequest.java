package com.mailsangja.core.dto.mail;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "메일 전송 요청")
public record MailSendRequest(
        @Schema(description = "보내는 사람. `user@example.com` 또는 `\"이름\" <user@example.com>` 형식", example = "\"홍길동\" <sender@gmail.com>")
        String from,

        @Schema(description = "답장 받을 주소. `user@example.com` 또는 `\"이름\" <user@example.com>` 형식", example = "\"홍길동\" <reply@gmail.com>")
        String replyTo,

        @Schema(description = "수신자 목록. multipart/form-data 에서는 to 필드를 반복 전달합니다.")
        List<String> to,

        @Schema(description = "참조 수신자 목록. multipart/form-data 에서는 cc 필드를 반복 전달합니다.")
        List<String> cc,

        @Schema(description = "숨은 참조 수신자 목록. multipart/form-data 에서는 bcc 필드를 반복 전달합니다.")
        List<String> bcc,

        @Schema(description = "메일 제목", example = "회의 자료 전달드립니다.")
        String subject,

        @Schema(description = "메일 본문", example = "안녕하세요.\n회의 자료 전달드립니다.")
        String content,

        @Schema(description = "첨부파일 목록. multipart/form-data 에서는 attachments 필드를 반복 전달합니다.")
        List<MultipartFile> attachments
) {
}
