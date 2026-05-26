package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GmailApiRateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailApiRateLimitService {

    public static final long THREAD_LIST_QUOTA_UNITS = 10;
    public static final long THREAD_GET_QUOTA_UNITS = 40;

    private static final String KEY_PREFIX = "gmail:rate-limit:user:";
    private static final long UNIT_SCALE = 1000;
    private static final long MILLIS_PER_MINUTE = 60000;
    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_per_minute = tonumber(ARGV[2])
            local now_ms = tonumber(ARGV[3])
            local cost = tonumber(ARGV[4])
            local ttl_ms = tonumber(ARGV[5])
            local millis_per_minute = tonumber(ARGV[6])

            local tokens = tonumber(redis.call('HGET', key, 'tokens'))
            local updated_at = tonumber(redis.call('HGET', key, 'updatedAtMillis'))

            if tokens == nil or updated_at == nil then
                tokens = capacity
                updated_at = now_ms
            end

            if now_ms < updated_at then
                updated_at = now_ms
            end

            local elapsed_ms = now_ms - updated_at
            local refill = math.floor((elapsed_ms * refill_per_minute) / millis_per_minute)
            tokens = math.min(capacity, tokens + refill)

            local allowed = 0
            if tokens >= cost then
                tokens = tokens - cost
                allowed = 1
            end

            redis.call('HSET', key, 'tokens', tostring(tokens), 'updatedAtMillis', tostring(now_ms))
            redis.call('PEXPIRE', key, ttl_ms)

            return allowed
            """, Long.class);

    private final GmailApiRateLimitProperties properties;
    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock = Clock.systemUTC();

    public void consumeThreadList(String accountKey) {
        consume(accountKey, THREAD_LIST_QUOTA_UNITS);
    }

    public void consumeThreadGet(String accountKey) {
        consume(accountKey, THREAD_GET_QUOTA_UNITS);
    }

    public void consume(String accountKey, long costUnits) {
        if (!properties.isEnabled()) {
            return;
        }
        validateInput(accountKey, costUnits);

        try {
            Long allowed = stringRedisTemplate.execute(
                    TOKEN_BUCKET_SCRIPT,
                    List.of(redisKey(accountKey)),
                    String.valueOf(toScaledUnits(properties.getPerUserCapacityUnits())),
                    String.valueOf(toScaledUnits(properties.getPerUserRefillUnitsPerMinute())),
                    String.valueOf(clock.millis()),
                    String.valueOf(toScaledUnits(costUnits)),
                    String.valueOf(properties.getKeyTtl().toMillis()),
                    String.valueOf(MILLIS_PER_MINUTE)
            );
            if (!Long.valueOf(1L).equals(allowed)) {
                throw new MailPushException(MailPushErrorCode.GMAIL_RATE_LIMIT_EXCEEDED);
            }
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            handleRedisFailure(e);
        }
    }

    private void validateInput(String accountKey, long costUnits) {
        if (isBlank(accountKey)
                || costUnits <= 0
                || properties.getPerUserCapacityUnits() <= 0
                || properties.getPerUserRefillUnitsPerMinute() <= 0
                || properties.getKeyTtl() == null
                || properties.getKeyTtl().isZero()
                || properties.getKeyTtl().isNegative()) {
            throw new MailPushException(MailPushErrorCode.GMAIL_RATE_LIMIT_CONFIG_INVALID);
        }
    }

    private long toScaledUnits(long units) {
        if (units > Long.MAX_VALUE / UNIT_SCALE) {
            throw new MailPushException(MailPushErrorCode.GMAIL_RATE_LIMIT_CONFIG_INVALID);
        }
        return units * UNIT_SCALE;
    }

    private String redisKey(String accountKey) {
        String encodedAccountKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(accountKey.getBytes(StandardCharsets.UTF_8));
        return KEY_PREFIX + encodedAccountKey;
    }

    private void handleRedisFailure(RuntimeException e) {
        if (properties.isFailOpen()) {
            log.warn("Gmail API rate limit Redis check failed. failOpen=true", e);
            return;
        }
        throw new MailPushException(MailPushErrorCode.GMAIL_RATE_LIMIT_UNAVAILABLE);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
