package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메일 계정 외관 변경 요청")
public record MailAccountAppearanceUpdateRequest(
        @Schema(description = "변경할 별칭", example = "업무 메일")
        String alias,
        @Schema(description = "변경할 아이콘", example = "mail")
        String icon,
        @Schema(description = "변경할 색상 HEX 값", example = "#4F46E5")
        String color
) {
    private static final String HEX_COLOR_REGEX = "^#[0-9A-Fa-f]{6}$";

    public void validate() {
        if (isBlank(alias) && isBlank(icon) && isBlank(color)) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NO_FIELD_TO_UPDATE);
        }
        if (!isBlank(alias) && alias.length() > 255) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_ALIAS);
        }
        if (!isBlank(icon) && icon.length() > 255) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_ICON);
        }
        if (!isBlank(color) && !color.matches(HEX_COLOR_REGEX)) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_COLOR);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
