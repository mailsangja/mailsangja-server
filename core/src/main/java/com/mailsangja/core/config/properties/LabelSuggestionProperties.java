package com.mailsangja.core.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "mailsangja.label.suggestion")
public class LabelSuggestionProperties {

    @Min(1)
    private int recentMailCount = 20;
}
