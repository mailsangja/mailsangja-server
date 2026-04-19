package com.mailsangja.worker.config.properties;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.gmail.push.local-ack")
public class GmailPushLocalAckProperties {

    private boolean enabled = false;
    private List<String> whitelistedErrorCodes = List.of();
    private Set<MailPushErrorCode> resolvedWhitelistedErrorCodes = Set.of();

    @PostConstruct
    public void validate() {
        if (!enabled) {
            resolvedWhitelistedErrorCodes = Set.of();
            return;
        }

        resolvedWhitelistedErrorCodes = resolveWhitelistedErrorCodes();
    }

    public boolean isWhitelisted(MailPushErrorCode errorCode) {
        if (!enabled || errorCode == null) {
            return false;
        }

        return resolvedWhitelistedErrorCodes.contains(errorCode);
    }

    private Set<MailPushErrorCode> resolveWhitelistedErrorCodes() {
        EnumSet<MailPushErrorCode> resolved = EnumSet.noneOf(MailPushErrorCode.class);

        for (String rawCode : whitelistedErrorCodes) {
            if (isBlank(rawCode)) {
                continue;
            }

            String normalizedCode = rawCode.trim();
            try {
                resolved.add(MailPushErrorCode.valueOf(normalizedCode));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Invalid mailsangja.gmail.push.local-ack.whitelisted-error-codes value: " + normalizedCode
                                + ". Supported values: " + Arrays.toString(MailPushErrorCode.values()),
                        e
                );
            }
        }

        return resolved;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
