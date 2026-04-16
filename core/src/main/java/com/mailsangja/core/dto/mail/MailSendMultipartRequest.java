package com.mailsangja.core.dto.mail;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "메일 전송 multipart 요청")
public record MailSendMultipartRequest(
        @Schema(description = "메일 metadata JSON")
        MailSendRequest metadata,

        @ArraySchema(
                arraySchema = @Schema(description = "첨부파일 목록. multipart/form-data 에서는 attachments 필드를 반복 전달합니다."),
                schema = @Schema(type = "string", format = "binary")
        )
        List<MultipartFile> attachments
) {
}
