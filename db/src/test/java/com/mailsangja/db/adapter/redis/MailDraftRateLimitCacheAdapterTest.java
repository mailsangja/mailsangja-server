package com.mailsangja.db.adapter.redis;

import com.mailsangja.db.common.redis.MailRateLimitProperties;
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

class MailDraftRateLimitCacheAdapterTest {

    @Test
    void tryConsumeWeeklyLimit_증가된카운트가설정값이하이면true를반환한다() {
        RedisFixture fixture = createFixture(20L, 20L);

        boolean result = fixture.adapter().tryConsumeWeeklyLimit(UUID.randomUUID());

        assertTrue(result);
        verify(fixture.operations()).increment(startsWith("MailDraft:rate:week:"));
    }

    @Test
    void tryConsumeWeeklyLimit_증가된카운트가설정값을초과하면false를반환한다() {
        RedisFixture fixture = createFixture(20L, 21L);

        boolean result = fixture.adapter().tryConsumeWeeklyLimit(UUID.randomUUID());

        assertFalse(result);
    }

    private RedisFixture createFixture(long weeklyDraftLimit, long incrementedCount) {
        MailRateLimitProperties properties = new MailRateLimitProperties();
        properties.setWeeklyDraftLimit(weeklyDraftLimit);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(operations);
        when(operations.increment(org.mockito.ArgumentMatchers.anyString())).thenReturn(incrementedCount);
        return new RedisFixture(new MailDraftRateLimitCacheAdapter(redisProvider(template), properties), operations);
    }

    private ObjectProvider<StringRedisTemplate> redisProvider(StringRedisTemplate template) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(template);
        return provider;
    }

    private record RedisFixture(MailDraftRateLimitCacheAdapter adapter, ValueOperations<String, String> operations) {
    }
}
