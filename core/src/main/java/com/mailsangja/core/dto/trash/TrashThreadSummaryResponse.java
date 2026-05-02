package com.mailsangja.core.dto.trash;

import com.mailsangja.core.dto.inbox.AttachmentResponse;
import com.mailsangja.core.dto.inbox.MailAddressResponse;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.ThreadLabelView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "휴지통 스레드 목록 항목")
public record TrashThreadSummaryResponse(
        @Schema(description = "스레드 내부 ID")
        UUID threadId,
        @Schema(description = "Gmail 스레드 ID", example = "18a2b3c4d5e6f7a8")
        String gmailThreadId,
        @Schema(description = "스레드가 속한 메일 계정 ID")
        UUID accountId,
        @Schema(description = "최신 메시지 제목", example = "프로젝트 미팅 일정 안내")
        String latestSubject,
        @Schema(description = "INBOUND일 때 발신자, OUTBOUND일 때 주 수신자")
        MailAddressResponse participant,
        @Schema(description = "최신 메시지 미리보기 텍스트", example = "안녕하세요, 다음 주 미팅 일정을 안내드립니다...")
        String snippet,
        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,
        @Schema(description = "최신 메시지 시각")
        LocalDateTime lastMessageAt,
        @Schema(description = "스레드 내 첨부파일 목록")
        List<AttachmentResponse> attachments,
        @Schema(description = "삭제된 메시지 수", example = "3")
        int messageCount,
        @Schema(description = "스레드에 적용된 라벨 목록")
        List<LabelSummary> labels
) {
    public record LabelSummary(
            @Schema(description = "라벨 ID") UUID labelId,
            @Schema(description = "라벨 이름") String name,
            @Schema(description = "라벨 색상 코드", example = "#FF5733") String colorCode
    ) {}

    public static TrashThreadSummaryResponse from(
            Thread representative,
            List<Attachment> attachments,
            int messageCount,
            Map<String, String> contactNameByEmail,
            List<ThreadLabelView> labelViews
    ) {
        return new TrashThreadSummaryResponse(
                representative.getId(),
                representative.getGmailThreadId(),
                representative.getMailAccount().getId(),
                representative.getLatestSubject(),
                MailAddressResponse.of(
                        representative.getLatestParticipantName(),
                        representative.getLatestParticipantAddress(),
                        contactNameByEmail
                ),
                representative.getLatestSnippet(),
                representative.isRead(),
                representative.getLastMessageAt(),
                attachments.stream().map(AttachmentResponse::from).toList(),
                messageCount,
                labelViews.stream()
                        .map(v -> new LabelSummary(v.labelId(), v.labelName(), v.colorCode()))
                        .toList()
        );
    }
}
