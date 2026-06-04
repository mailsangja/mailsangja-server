package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "연결된 메일 계정 응답")
public record MailAccountResponse(
        @Schema(description = "메일 계정 ID")
        UUID id,
        @Schema(description = "서비스 사용자 ID")
        UUID userId,
        @Schema(description = "메일 제공자", example = "GMAIL")
        MailProvider provider,
        @Schema(description = "연결된 메일 주소", example = "user@gmail.com")
        String emailAddress,
        @Schema(description = "사용자가 지정한 메일 계정 별칭", example = "업무 메일")
        String alias,
        @Schema(description = "메일 계정 아이콘", example = "mail")
        String icon,
        @Schema(description = "메일 계정 색상 HEX 값", example = "#4F46E5")
        String color,
        @Schema(description = "메일 계정 활성 여부", example = "true")
        boolean active,
        @Schema(description = "메일 계정 재연동 필요 여부", example = "false")
        boolean reauthRequired,
        @Schema(description = "메일 동기화 히스토리 ID", nullable = true)
        String syncHistoryId,
        @Schema(description = "Gmail watch 만료 시각", nullable = true)
        LocalDateTime watchExpiresAt
) {
    public static MailAccountResponse from(MailAccount mailAccount) {
        return new MailAccountResponse(
                mailAccount.getId(),
                mailAccount.getUser().getId(),
                mailAccount.getProvider(),
                mailAccount.getEmailAddress(),
                mailAccount.getAlias(),
                mailAccount.getIcon(),
                mailAccount.getColor(),
                mailAccount.isActive(),
                isBlank(mailAccount.getRefreshToken()),
                mailAccount.getSyncHistoryId(),
                mailAccount.getWatchExpiresAt()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
