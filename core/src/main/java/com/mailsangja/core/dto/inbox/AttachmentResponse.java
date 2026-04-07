package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "첨부파일 정보")
public record AttachmentResponse(
        @Schema(description = "첨부파일 ID")
        UUID id,
        @Schema(description = "Gmail 첨부파일 ID (다운로드 시 사용)", example = "ANGjdJ8...")
        String gmailAttachmentId,
        @Schema(description = "파일명", example = "document.pdf")
        String filename,
        @Schema(description = "MIME 타입", example = "application/pdf")
        String mimeType,
        @Schema(description = "파일 크기 (bytes)", example = "102400")
        Integer size
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getGmailAttachmentId(),
                attachment.getFilename(),
                attachment.getMimeType(),
                attachment.getSize()
        );
    }
}
