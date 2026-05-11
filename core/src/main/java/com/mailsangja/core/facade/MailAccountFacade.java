package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.contact.GoogleContactResult;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
import com.mailsangja.core.dto.mail.InitialMailSyncMessage;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountListResponse;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.core.service.contact.ContactCommandService;
import com.mailsangja.core.service.google.GoogleMailWatchQueryService;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.core.service.google.GooglePeopleContactQueryService;
import com.mailsangja.core.service.mail.InitialMailSyncMessageCommandService;
import com.mailsangja.core.service.mail.MailAccountCommandService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailAccountFacade {

    private static final String HEX_COLOR_REGEX = "^#[0-9A-Fa-f]{6}$";
    private static final String DEFAULT_ICON = "good";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MailAccountCommandService mailAccountCommandService;
    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleOAuthQueryService googleOAuthQueryService;
    private final GoogleMailWatchQueryService googleMailWatchQueryService;
    private final InitialMailSyncMessageCommandService initialMailSyncMessageCommandService;
    private final GooglePeopleContactQueryService googlePeopleContactQueryService;
    private final ContactCommandService contactCommandService;

    public MailAccountAuthorizeResponse authorizeGoogle(String state) {
        String authorizationUrl = googleOAuthQueryService.buildAuthorizationUrl(state);
        return new MailAccountAuthorizeResponse(authorizationUrl);
    }

    public MailAccountResponse handleGoogleCallback(
            User user,
            String code,
            String alias,
            String icon,
            String color
    ) {
        validateAuthorizationCode(code);

        GoogleMailAccountResult result = googleOAuthQueryService.getGoogleMailAccountResult(code);
        MailAccountAppearance appearance = normalizeMailAccountAppearance(result.emailAddress(), alias, icon, color);

        mailAccountCommandService.validateGoogleMailAccountCreation(user, result);

        GoogleMailWatchResult watchResult = googleMailWatchQueryService.watch(result.accessToken());

        MailAccount savedMailAccount = mailAccountCommandService.createGoogleMailAccount(
                user,
                result,
                appearance.alias(),
                appearance.icon(),
                appearance.color(),
                watchResult
        );

        if (savedMailAccount.getProvider() == MailProvider.GMAIL) {
            InitialMailSyncMessage initialMailSyncMessage = InitialMailSyncMessage.from(savedMailAccount);
            initialMailSyncMessageCommandService.publish(initialMailSyncMessage);
            saveInitialGoogleContacts(user, result.accessToken(), savedMailAccount);
        }

        return MailAccountResponse.from(savedMailAccount);
    }

    public List<MailAccountListResponse> getMyMailAccounts(User user) {
        List<MailAccount> mailAccounts = mailAccountQueryService.findAllByUserId(user.getId());
        return mailAccounts.stream()
                .map(MailAccountListResponse::from)
                .toList();
    }

    private void validateAuthorizationCode(String code) {
        if (isBlank(code) || code.contains(" ") || code.length() > 2048) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_AUTHORIZATION_CODE);
        }
    }

    private MailAccountAppearance normalizeMailAccountAppearance(
            String emailAddress,
            String alias,
            String icon,
            String color
    ) {
        String normalizedAlias = isBlank(alias) ? emailAddress : alias;
        String normalizedIcon = isBlank(icon) ? DEFAULT_ICON : icon;
        String normalizedColor = isBlank(color) ? generateRandomHexColor() : color;

        validateMailAccountAppearance(normalizedAlias, normalizedIcon, normalizedColor);
        return new MailAccountAppearance(normalizedAlias, normalizedIcon, normalizedColor);
    }

    private void validateMailAccountAppearance(String alias, String icon, String color) {
        if (isBlank(alias) || alias.length() > 255) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_ALIAS);
        }

        if (isBlank(icon) || icon.length() > 255) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_ICON);
        }

        if (!color.matches(HEX_COLOR_REGEX)) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_MAIL_ACCOUNT_COLOR);
        }
    }

    private String generateRandomHexColor() {
        return String.format(
                "#%02X%02X%02X",
                SECURE_RANDOM.nextInt(256),
                SECURE_RANDOM.nextInt(256),
                SECURE_RANDOM.nextInt(256)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void saveInitialGoogleContacts(User user, String accessToken, MailAccount mailAccount) {
        try {
            List<GoogleContactResult> contacts = googlePeopleContactQueryService.getContacts(accessToken);
            int savedCount = contactCommandService.saveMissingContacts(user, contacts);
            log.info(
                    "Saved initial Google contacts. mailAccountId={} userId={} fetchedCount={} savedCount={}",
                    mailAccount.getId(),
                    user.getId(),
                    contacts.size(),
                    savedCount
            );
        } catch (Exception e) {
            log.warn(
                    "Failed to save initial Google contacts. mailAccountId={} userId={}",
                    mailAccount.getId(),
                    user.getId(),
                    e
            );
        }
    }

    private record MailAccountAppearance(
            String alias,
            String icon,
            String color
    ) {
    }
}
