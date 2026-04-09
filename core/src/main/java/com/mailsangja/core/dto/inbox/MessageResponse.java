package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "메일 메시지 상세 정보")
public record MessageResponse(
        @Schema(description = "메시지 ID")
        UUID id,
        @Schema(description = "Gmail 메시지 ID", example = "18a2b3c4d5e6f7a8")
        String gmailMessageId,
        @Schema(description = "제목", example = "Re: 프로젝트 미팅 일정")
        String subject,
        @Schema(description = "메시지 방향 (INBOUND: 수신, OUTBOUND: 발신)")
        Direction direction,
        @Schema(description = "발신자")
        MailAddressResponse from,
        @Schema(description = "수신자 목록")
        List<MailAddressResponse> to,
        @Schema(description = "CC 수신자 목록")
        List<MailAddressResponse> cc,
        @Schema(description = "메시지 스니펫 (미리보기)", example = "안녕하세요, 미팅 관련하여...")
        String snippet,
        @Schema(description = "읽음 여부", example = "true")
        boolean isRead,
        @Schema(description = "발송/수신 시각")
        LocalDateTime sentAt,
        @Schema(description = "본문 plain text")
        String bodyText,
        @Schema(description = "본문 HTML")
        String bodyHtml,
        @Schema(description = "첨부파일 목록")
        List<AttachmentResponse> attachments
) {
    public static MessageResponse from(Message message, Map<String, String> contactNameByEmail) {
        List<AttachmentResponse> attachmentResponses = message.getAttachments().stream()
                .map(AttachmentResponse::from)
                .toList();

        List<MailAddressResponse> toAddresses = message.getToAddresses() == null ? List.of() :
                message.getToAddresses().stream()
                        .map(email -> MailAddressResponse.of(email, contactNameByEmail))
                        .toList();

        List<MailAddressResponse> ccAddresses = message.getCcAddresses() == null ? List.of() :
                message.getCcAddresses().stream()
                        .map(email -> MailAddressResponse.of(email, contactNameByEmail))
                        .toList();

        return new MessageResponse(
                message.getId(),
                message.getGmailMessageId(),
                message.getSubject(),
                message.getDirection(),
                MailAddressResponse.of(message.getFromAddress(), contactNameByEmail),
                toAddresses,
                ccAddresses,
                message.getSnippet(),
                message.isRead(),
                message.getSentAt(),
                message.getBodyText(),
                message.getBodyHtml(),
                attachmentResponses
        );
    }
}
