package com.mailsangja.core.facade.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountCreateCommand;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.core.service.mail.MailAccountCommandService;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MailAccountFacade {

    private final MailAccountCommandService mailAccountCommandService;

    public MailAccountAuthorizeResponse authorizeGoogle(String state) {
        String authorizationUrl = "https://accounts.google.com/o/oauth2/v2/auth?state=" + state;
        return new MailAccountAuthorizeResponse(authorizationUrl);
    }

    public MailAccountResponse handleGoogleCallback(User user, String code) {
        validateAuthorizationCode(code);

        GoogleMailAccountResult result = createStubGoogleMailAccountResult(code);

        MailAccountCreateCommand command = new MailAccountCreateCommand(
                MailProvider.GMAIL,
                result.emailAddress(),
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                null
        );
        validateCreateCommand(command);

        return MailAccountResponse.from(mailAccountCommandService.create(user, command));
    }

    private GoogleMailAccountResult createStubGoogleMailAccountResult(String code) {
        String normalizedCode = code == null ? "unknown" : code;
        return new GoogleMailAccountResult(
                "stub-" + normalizedCode + "@gmail.com",
                "stub-access-token",
                LocalDateTime.now().plusHours(1),
                "stub-refresh-token"
        );
    }

    private void validateAuthorizationCode(String code) {
        if (isBlank(code) || code.contains(" ") || code.length() > 2048) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_AUTHORIZATION_CODE);
        }
    }

    private void validateCreateCommand(MailAccountCreateCommand command) {
        if (command.provider() != MailProvider.GMAIL) {
            throw new MailAccountException(MailAccountErrorCode.UNSUPPORTED_MAIL_PROVIDER);
        }

        if (isBlank(command.emailAddress())
                || isBlank(command.accessToken())
                || command.accessTokenExpiresAt() == null
                || isBlank(command.refreshToken())) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
