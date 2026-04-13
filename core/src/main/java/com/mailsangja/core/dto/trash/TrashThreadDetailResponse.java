package com.mailsangja.core.dto.trash;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "휴지통 스레드 상세")
public record TrashThreadDetailResponse(
        @Schema(description = "스레드 내부 ID")
        UUID threadId,
        @Schema(description = "Gmail 스레드 ID", example = "18a2b3c4d5e6f7a8")
        String gmailThreadId,
        @Schema(description = "스레드가 속한 메일 계정 ID")
        UUID accountId,
        @Schema(description = "메일 계정 이메일 주소", example = "user@gmail.com")
        String accountEmail,
        @Schema(description = "해당 대화의 삭제된 메시지 목록 (sentAt 오름차순)")
        List<TrashMessageItemResponse> messages
) {
    public static TrashThreadDetailResponse from(Thread thread, List<Message> messages) {
        return new TrashThreadDetailResponse(
                thread.getId(),
                thread.getGmailThreadId(),
                thread.getMailAccount().getId(),
                thread.getMailAccount().getEmailAddress(),
                messages.stream().map(TrashMessageItemResponse::from).toList()
        );
    }
}
