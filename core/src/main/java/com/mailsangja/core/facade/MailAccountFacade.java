package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeRequest;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.core.service.mail.MailAccountCommandService;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailAccountFacade {

    private static final String HEX_COLOR_REGEX = "^#[0-9A-Fa-f]{6}$";

    private final MailAccountCommandService mailAccountCommandService;
    private final GoogleOAuthQueryService googleOAuthQueryService;

    public MailAccountAuthorizeResponse authorizeGoogle(String state, MailAccountAuthorizeRequest request) {
        validateAuthorizeRequest(request);

        String authorizationUrl = googleOAuthQueryService.buildAuthorizationUrl(state);
        return new MailAccountAuthorizeResponse(authorizationUrl);
    }

    public MailAccountResponse handleGoogleCallback(User user, String code, String icon, String color) {
        validateAuthorizationCode(code);
        validateMailAccountAppearance(icon, color);

        GoogleMailAccountResult result = googleOAuthQueryService.getGoogleMailAccountResult(code);
        return MailAccountResponse.from(mailAccountCommandService.createGoogleMailAccount(user, result, icon, color));
    }

    private void validateAuthorizeRequest(MailAccountAuthorizeRequest request) {
        if (request == null) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }

        validateMailAccountAppearance(request.icon(), request.color());
    }

    private void validateAuthorizationCode(String code) {
        if (isBlank(code) || code.contains(" ") || code.length() > 2048) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_AUTHORIZATION_CODE);
        }
    }

    private void validateMailAccountAppearance(String icon, String color) {
        if (isBlank(icon)) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_ICON);
        }

        if (isBlank(color) || !color.matches(HEX_COLOR_REGEX)) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_COLOR);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
