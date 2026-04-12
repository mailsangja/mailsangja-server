package com.mailsangja.core.dto.trash;

import com.mailsangja.db.entity.mail.Thread;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "휴지통 스레드 목록 항목")
public record TrashThreadSummaryResponse(
        @Schema(description = "스레드 내부 ID")
        UUID threadId,
        @Schema(description = "Gmail 스레드 ID", example = "18a2b3c4d5e6f7a8")
        String gmailThreadId,
        @Schema(description = "스레드가 속한 메일 계정 ID")
        UUID accountId,
        @Schema(description = "메일 계정 이메일 주소", example = "user@gmail.com")
        String accountEmail,
        @Schema(description = "최신 메시지 제목", example = "프로젝트 미팅 일정 안내")
        String latestSubject,
        @Schema(description = "최신 메시지 미리보기 텍스트", example = "안녕하세요, 다음 주 미팅 일정을 안내드립니다...")
        String snippet,
        @Schema(description = "최신 메시지 시각")
        LocalDateTime lastMessageAt,
        @Schema(description = "삭제된 시각")
        LocalDateTime deletedAt
) {
    public static TrashThreadSummaryResponse from(Thread thread) {
        return new TrashThreadSummaryResponse(
                thread.getId(),
                thread.getGmailThreadId(),
                thread.getMailAccount().getId(),
                thread.getMailAccount().getEmailAddress(),
                thread.getLatestSubject(),
                thread.getLatestSnippet(),
                thread.getLastMessageAt(),
                thread.getDeletedAt()
        );
    }
}
