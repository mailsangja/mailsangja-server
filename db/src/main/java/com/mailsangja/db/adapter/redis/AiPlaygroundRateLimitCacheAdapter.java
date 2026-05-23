package com.mailsangja.db.adapter.redis;

import com.mailsangja.db.common.redis.MailRateLimitProperties;
import com.mailsangja.db.port.AiPlaygroundRateLimitCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mailsangja.redis.rate-limit.enabled", havingValue = "true")
public class AiPlaygroundRateLimitCacheAdapter implements AiPlaygroundRateLimitCachePort {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final WeekFields ISO_WEEK_FIELDS = WeekFields.ISO;
    private static final String KEY_PREFIX = "AiPlayground:rate:week:";

    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final MailRateLimitProperties properties;

    @Override
    public boolean tryConsumeWeeklyLimit(UUID userId) {
        String key = weeklyKey(userId);
        StringRedisTemplate template = stringRedisTemplate();
        Long count = template.opsForValue().increment(key);
        expireIfFirstCount(key, count);
        return isWeeklyLimitAllowed(count);
    }

    private StringRedisTemplate stringRedisTemplate() {
        return stringRedisTemplateProvider.getObject();
    }

    private boolean isWeeklyLimitAllowed(Long count) {
        return safeCount(count) <= properties.getWeeklyPlaygroundLimit();
    }

    private String weeklyKey(UUID userId) {
        LocalDate today = LocalDate.now(KST_ZONE_ID);
        int year = today.getYear();
        int week = today.get(ISO_WEEK_FIELDS.weekOfWeekBasedYear());
        return KEY_PREFIX + userId + ":" + String.format("%04d%02d", year, week);
    }

    private void expireIfFirstCount(String key, Long count) {
        if (count != null && count == 1L) {
            stringRedisTemplate().expireAt(key, nextWeekStart());
        }
    }

    private java.time.Instant nextWeekStart() {
        LocalDate today = LocalDate.now(KST_ZONE_ID);
        LocalDate nextMonday = today.with(DayOfWeek.MONDAY).plusWeeks(1);
        return nextMonday.atStartOfDay(KST_ZONE_ID).toInstant();
    }

    private long safeCount(Long count) {
        if (count == null) {
            return 0L;
        }
        return count;
    }
}
