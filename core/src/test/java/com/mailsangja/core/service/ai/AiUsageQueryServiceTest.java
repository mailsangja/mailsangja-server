package com.mailsangja.core.service.ai;

import com.mailsangja.core.dto.ai.AiUsageItemResponse;
import com.mailsangja.core.dto.ai.AiUsageType;
import com.mailsangja.db.common.redis.MailRateLimitProperties;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.port.LabelSuggestionRateLimitCachePort;
import com.mailsangja.db.port.MailDraftRateLimitCachePort;
import com.mailsangja.db.port.MailReviewRateLimitCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiUsageQueryServiceTest {

    private MailDraftRateLimitCachePort draftPort;
    private MailReviewRateLimitCachePort reviewPort;
    private LabelSuggestionRateLimitCachePort suggestionPort;
    private MailRateLimitProperties properties;
    private AiUsageQueryService service;

    @BeforeEach
    void setUp() {
        draftPort = mock(MailDraftRateLimitCachePort.class);
        reviewPort = mock(MailReviewRateLimitCachePort.class);
        suggestionPort = mock(LabelSuggestionRateLimitCachePort.class);

        properties = new MailRateLimitProperties();
        properties.setWeeklyDraftLimit(10L);
        properties.setWeeklyReviewLimit(10L);
        properties.setWeeklySuggestionLimit(2L);
        properties.setWeeklyProDraftLimit(30L);
        properties.setWeeklyProReviewLimit(30L);
        properties.setWeeklyProSuggestionLimit(15L);

        service = new AiUsageQueryService(draftPort, reviewPort, suggestionPort, properties);
    }

    @Test
    void getMailDraftUsage_FREE_플랜이면_FREE_한도를반환한다() {
        UUID userId = UUID.randomUUID();
        when(draftPort.getWeeklyUsage(userId)).thenReturn(3L);

        AiUsageItemResponse response = service.getMailDraftUsage(userId, Plan.FREE);

        assertEquals(AiUsageType.MAIL_DRAFT, response.type());
        assertEquals(3L, response.used());
        assertEquals(10L, response.limit());
    }

    @Test
    void getMailDraftUsage_PRO_플랜이면_PRO_한도를반환한다() {
        UUID userId = UUID.randomUUID();
        when(draftPort.getWeeklyUsage(userId)).thenReturn(5L);

        AiUsageItemResponse response = service.getMailDraftUsage(userId, Plan.PRO);

        assertEquals(AiUsageType.MAIL_DRAFT, response.type());
        assertEquals(5L, response.used());
        assertEquals(30L, response.limit());
    }

    @Test
    void getMailReviewUsage_FREE_플랜이면_FREE_한도를반환한다() {
        UUID userId = UUID.randomUUID();
        when(reviewPort.getWeeklyUsage(userId)).thenReturn(1L);

        AiUsageItemResponse response = service.getMailReviewUsage(userId, Plan.FREE);

        assertEquals(AiUsageType.MAIL_REVIEW, response.type());
        assertEquals(1L, response.used());
        assertEquals(10L, response.limit());
    }

    @Test
    void getMailReviewUsage_PRO_플랜이면_PRO_한도를반환한다() {
        UUID userId = UUID.randomUUID();
        when(reviewPort.getWeeklyUsage(userId)).thenReturn(10L);

        AiUsageItemResponse response = service.getMailReviewUsage(userId, Plan.PRO);

        assertEquals(AiUsageType.MAIL_REVIEW, response.type());
        assertEquals(10L, response.used());
        assertEquals(30L, response.limit());
    }

    @Test
    void getLabelSuggestionUsage_FREE_플랜이면_FREE_한도를반환한다() {
        UUID userId = UUID.randomUUID();
        when(suggestionPort.getWeeklyUsage(userId)).thenReturn(0L);

        AiUsageItemResponse response = service.getLabelSuggestionUsage(userId, Plan.FREE);

        assertEquals(AiUsageType.LABEL_SUGGESTION, response.type());
        assertEquals(0L, response.used());
        assertEquals(2L, response.limit());
    }

    @Test
    void getLabelSuggestionUsage_PRO_플랜이면_PRO_한도를반환한다() {
        UUID userId = UUID.randomUUID();
        when(suggestionPort.getWeeklyUsage(userId)).thenReturn(7L);

        AiUsageItemResponse response = service.getLabelSuggestionUsage(userId, Plan.PRO);

        assertEquals(AiUsageType.LABEL_SUGGESTION, response.type());
        assertEquals(7L, response.used());
        assertEquals(15L, response.limit());
    }
}
