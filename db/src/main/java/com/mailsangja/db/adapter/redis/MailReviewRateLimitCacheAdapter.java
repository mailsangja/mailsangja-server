package com.mailsangja.db.adapter.redis;

import com.mailsangja.db.common.redis.MailDraftRateLimitProperties;
import com.mailsangja.db.port.MailReviewRateLimitCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mailsangja.redis.rate-limit.enabled", havingValue = "true")
public class MailReviewRateLimitCacheAdapter implements MailReviewRateLimitCachePort {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String KEY_PREFIX = "MailReview:rate:month:";

    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final MailDraftRateLimitProperties properties;

    @Override
    public boolean tryConsumeMonthlyLimit(UUID userId) {
        String key = monthlyKey(userId);
        StringRedisTemplate template = stringRedisTemplate();
        Long count = template.opsForValue().increment(key);
        expireIfFirstCount(key, count);
        return isMonthlyLimitAllowed(count);
    }

    private StringRedisTemplate stringRedisTemplate() {
        return stringRedisTemplateProvider.getObject();
    }

    private boolean isMonthlyLimitAllowed(Long count) {
        return safeCount(count) <= properties.getReviewMonthlyLimit();
    }

    private String monthlyKey(UUID userId) {
        LocalDate today = LocalDate.now(KST_ZONE_ID);
        return KEY_PREFIX + userId + ":" + today.format(MONTH_FORMAT);
    }

    private void expireIfFirstCount(String key, Long count) {
        if (count != null && count == 1L) {
            stringRedisTemplate().expireAt(key, nextMonthStart());
        }
    }

    private java.time.Instant nextMonthStart() {
        LocalDate nextMonth = LocalDate.now(KST_ZONE_ID).plusMonths(1).withDayOfMonth(1);
        return nextMonth.atStartOfDay(KST_ZONE_ID).toInstant();
    }

    private long safeCount(Long count) {
        if (count == null) {
            return 0L;
        }
        return count;
    }
}
