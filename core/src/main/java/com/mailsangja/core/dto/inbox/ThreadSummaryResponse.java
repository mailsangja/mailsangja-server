package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Thread;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "통합 인박스 스레드 목록 항목")
public record ThreadSummaryResponse(
        @Schema(description = "스레드 내부 ID")
        UUID threadId,
        @Schema(description = "Gmail 스레드 ID", example = "18a2b3c4d5e6f7a8")
        String gmailThreadId,
        @Schema(description = "스레드가 속한 메일 계정 ID")
        UUID accountId,
        @Schema(description = "최신 메시지 제목", example = "프로젝트 미팅 일정 안내")
        String latestSubject,
        @Schema(description = "INBOUND일 때 발신자 이메일, OUTBOUND일 때 주 수신자 이메일", example = "hong@example.com")
        String participantAddress,
        @Schema(description = "최신 메시지 미리보기 텍스트", example = "안녕하세요, 다음 주 미팅 일정을 안내드립니다...")
        String snippet,
        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,
        @Schema(description = "최신 메시지 시각")
        LocalDateTime lastMessageAt,
        @Schema(description = "스레드 내 첨부파일 목록")
        List<AttachmentResponse> attachments
) {
    public static ThreadSummaryResponse from(Thread thread, List<Attachment> attachments) {
        List<AttachmentResponse> attachmentResponses = attachments.stream()
                .map(AttachmentResponse::from)
                .toList();

        return new ThreadSummaryResponse(
                thread.getId(),
                thread.getGmailThreadId(),
                thread.getMailAccount().getId(),
                thread.getLatestSubject(),
                thread.getLatestParticipantAddress(),
                thread.getLatestSnippet(),
                thread.isRead(),
                thread.getLastMessageAt(),
                attachmentResponses
        );
    }
}
