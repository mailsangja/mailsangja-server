package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "메일 계정 목록 조회 응답")
public record MailAccountListResponse(
        @Schema(description = "메일 계정 ID")
        UUID id,
        @Schema(description = "메일 계정 활성 여부", example = "true")
        boolean isActive,
        @Schema(description = "메일 계정 재연동 필요 여부", example = "false")
        boolean reauthorizationRequired,
        @Schema(description = "메일 제공자", example = "GMAIL")
        MailProvider provider,
        @Schema(description = "연결된 메일 주소", example = "user@gmail.com")
        String emailAddress,
        @Schema(description = "사용자가 지정한 메일 계정 별칭", example = "업무 메일")
        String alias,
        @Schema(description = "메일 계정 색상 HEX 값", example = "#4F46E5")
        String color,
        @Schema(description = "메일 계정 아이콘", example = "mail")
        String icon
) {
    public static MailAccountListResponse from(MailAccount mailAccount) {
        return new MailAccountListResponse(
                mailAccount.getId(),
                mailAccount.isActive(),
                isReauthorizationRequired(mailAccount),
                mailAccount.getProvider(),
                mailAccount.getEmailAddress(),
                mailAccount.getAlias(),
                mailAccount.getColor(),
                mailAccount.getIcon()
        );
    }

    private static boolean isReauthorizationRequired(MailAccount mailAccount) {
        return mailAccount.getProvider() == MailProvider.GMAIL
                && isBlank(mailAccount.getRefreshToken());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
