package com.mailsangja.core.dto.trash;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "휴지통 메시지 항목")
public record TrashMessageItemResponse(
        @Schema(description = "메시지 내부 ID")
        UUID messageId,
        @Schema(description = "Gmail 메시지 ID", example = "18a2b3c4d5e6f7a8")
        String gmailMessageId,
        @Schema(description = "메시지 방향 (INBOUND: 받은 메일, OUTBOUND: 보낸 메일)", example = "INBOUND")
        Direction direction,
        @Schema(description = "제목", example = "프로젝트 미팅 일정 안내")
        String subject,
        @Schema(description = "발신자 이메일", example = "sender@example.com")
        String fromAddress,
        @Schema(description = "수신자 이메일 목록")
        List<String> toAddresses,
        @Schema(description = "미리보기 텍스트", example = "안녕하세요, 다음 주 미팅 일정을 안내드립니다...")
        String snippet,
        @Schema(description = "발송 시각")
        LocalDateTime sentAt,
        @Schema(description = "삭제된 시각")
        LocalDateTime deletedAt
) {
    public static TrashMessageItemResponse from(Message message) {
        return new TrashMessageItemResponse(
                message.getId(),
                message.getGmailMessageId(),
                message.getDirection(),
                message.getSubject(),
                message.getFromAddress(),
                message.getToAddresses(),
                message.getSnippet(),
                message.getSentAt(),
                message.getDeletedAt()
        );
    }
}
