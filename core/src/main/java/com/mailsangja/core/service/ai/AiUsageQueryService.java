package com.mailsangja.core.service.ai;

import com.mailsangja.core.dto.ai.AiUsageItemResponse;
import com.mailsangja.core.dto.ai.AiUsageType;
import com.mailsangja.db.common.redis.MailRateLimitProperties;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.port.LabelSuggestionRateLimitCachePort;
import com.mailsangja.db.port.MailDraftRateLimitCachePort;
import com.mailsangja.db.port.MailReviewRateLimitCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiUsageQueryService {

    private final MailDraftRateLimitCachePort mailDraftRateLimitCachePort;
    private final MailReviewRateLimitCachePort mailReviewRateLimitCachePort;
    private final LabelSuggestionRateLimitCachePort labelSuggestionRateLimitCachePort;
    private final MailRateLimitProperties mailRateLimitProperties;

    public AiUsageItemResponse getMailDraftUsage(UUID userId, Plan plan) {
        long used = mailDraftRateLimitCachePort.getWeeklyUsage(userId);
        long limit = plan == Plan.PRO
                ? mailRateLimitProperties.getWeeklyProDraftLimit()
                : mailRateLimitProperties.getWeeklyDraftLimit();
        return new AiUsageItemResponse(AiUsageType.MAIL_DRAFT, used, limit);
    }

    public AiUsageItemResponse getMailReviewUsage(UUID userId, Plan plan) {
        long used = mailReviewRateLimitCachePort.getWeeklyUsage(userId);
        long limit = plan == Plan.PRO
                ? mailRateLimitProperties.getWeeklyProReviewLimit()
                : mailRateLimitProperties.getWeeklyReviewLimit();
        return new AiUsageItemResponse(AiUsageType.MAIL_REVIEW, used, limit);
    }

    public AiUsageItemResponse getLabelSuggestionUsage(UUID userId, Plan plan) {
        long used = labelSuggestionRateLimitCachePort.getWeeklyUsage(userId);
        long limit = plan == Plan.PRO
                ? mailRateLimitProperties.getWeeklyProSuggestionLimit()
                : mailRateLimitProperties.getWeeklySuggestionLimit();
        return new AiUsageItemResponse(AiUsageType.LABEL_SUGGESTION, used, limit);
    }
}
