package com.mailsangja.db.adapter.redis;

import com.mailsangja.db.common.redis.MailDraftRateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailReviewRateLimitCacheAdapterTest {

    @Test
    void tryConsumeMonthlyLimit_증가된카운트가review설정값이하이면true를반환한다() {
        RedisFixture fixture = createFixture(10L, 10L);

        boolean result = fixture.adapter().tryConsumeMonthlyLimit(UUID.randomUUID());

        assertTrue(result);
        verify(fixture.operations()).increment(startsWith("MailReview:rate:month:"));
    }

    @Test
    void tryConsumeMonthlyLimit_증가된카운트가review설정값을초과하면false를반환한다() {
        RedisFixture fixture = createFixture(10L, 11L);

        boolean result = fixture.adapter().tryConsumeMonthlyLimit(UUID.randomUUID());

        assertFalse(result);
    }

    private RedisFixture createFixture(long reviewMonthlyLimit, long incrementedCount) {
        MailDraftRateLimitProperties properties = new MailDraftRateLimitProperties();
        properties.setReviewMonthlyLimit(reviewMonthlyLimit);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(operations);
        when(operations.increment(org.mockito.ArgumentMatchers.anyString())).thenReturn(incrementedCount);
        return new RedisFixture(new MailReviewRateLimitCacheAdapter(redisProvider(template), properties), operations);
    }

    private ObjectProvider<StringRedisTemplate> redisProvider(StringRedisTemplate template) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(template);
        return provider;
    }

    private record RedisFixture(MailReviewRateLimitCacheAdapter adapter, ValueOperations<String, String> operations) {
    }
}
