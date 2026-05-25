package com.mailsangja.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.mail-account")
public class MailAccountProperties {

    private int maxCount = 2;
}
