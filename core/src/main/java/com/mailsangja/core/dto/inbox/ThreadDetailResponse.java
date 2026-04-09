package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "메일 스레드 상세 응답")
public record ThreadDetailResponse(
        @Schema(description = "스레드 내부 ID")
        UUID threadId,
        @Schema(description = "Gmail 스레드 ID", example = "18a2b3c4d5e6f7a8")
        String gmailThreadId,
        @Schema(description = "스레드가 속한 메일 계정 ID")
        UUID accountId,
        @Schema(description = "최신 메시지 제목", example = "프로젝트 미팅 일정 안내")
        String latestSubject,
        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,
        @Schema(description = "최신 메시지 시각")
        LocalDateTime lastMessageAt,
        @Schema(description = "스레드 내 메시지 목록 (sentAt ASC 정렬)")
        List<MessageResponse> messages
) {
    public static ThreadDetailResponse from(Thread thread, List<Message> messages) {
        List<MessageResponse> messageResponses = messages.stream()
                .map(MessageResponse::from)
                .toList();

        return new ThreadDetailResponse(
                thread.getId(),
                thread.getGmailThreadId(),
                thread.getMailAccount().getId(),
                thread.getLatestSubject(),
                thread.isRead(),
                thread.getLastMessageAt(),
                messageResponses
        );
    }
}
