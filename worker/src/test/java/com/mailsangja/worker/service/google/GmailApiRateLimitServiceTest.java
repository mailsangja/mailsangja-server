package com.mailsangja.worker.service.google;

import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GmailApiRateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
class GmailApiRateLimitServiceTest {

    @Test
    void consume_토큰이충분하면허용한다() {
        GmailApiRateLimitProperties properties = createProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doReturn(1L).when(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));
        GmailApiRateLimitService service = new GmailApiRateLimitService(properties, redisTemplate);

        service.consume("alice@example.com", 40);

        verify(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    void consume_토큰이부족하면예외를던진다() {
        GmailApiRateLimitProperties properties = createProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doReturn(0L).when(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));
        GmailApiRateLimitService service = new GmailApiRateLimitService(properties, redisTemplate);

        MailPushException exception = assertThrows(MailPushException.class, () -> service.consume("alice@example.com", 40));

        assertEquals(MailPushErrorCode.GMAIL_RATE_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void consume_disabled이면Redis를호출하지않는다() {
        GmailApiRateLimitProperties properties = createProperties();
        properties.setEnabled(false);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        GmailApiRateLimitService service = new GmailApiRateLimitService(properties, redisTemplate);

        service.consume("alice@example.com", 40);

        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    void consume_Redis장애이고FailOpen이면허용한다() {
        GmailApiRateLimitProperties properties = createProperties();
        properties.setFailOpen(true);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doThrow(new RedisConnectionFailureException("down"))
                .when(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));
        GmailApiRateLimitService service = new GmailApiRateLimitService(properties, redisTemplate);

        service.consume("alice@example.com", 40);
    }

    @Test
    void consume_Redis장애이고FailClosed이면예외를던진다() {
        GmailApiRateLimitProperties properties = createProperties();
        properties.setFailOpen(false);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doThrow(new RedisConnectionFailureException("down"))
                .when(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));
        GmailApiRateLimitService service = new GmailApiRateLimitService(properties, redisTemplate);

        MailPushException exception = assertThrows(MailPushException.class, () -> service.consume("alice@example.com", 40));

        assertEquals(MailPushErrorCode.GMAIL_RATE_LIMIT_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void consume_설정이잘못되면예외를던진다() {
        GmailApiRateLimitProperties properties = createProperties();
        properties.setPerUserCapacityUnits(0);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        GmailApiRateLimitService service = new GmailApiRateLimitService(properties, redisTemplate);

        MailPushException exception = assertThrows(MailPushException.class, () -> service.consume("alice@example.com", 40));

        assertEquals(MailPushErrorCode.GMAIL_RATE_LIMIT_CONFIG_INVALID, exception.getErrorCode());
    }

    private GmailApiRateLimitProperties createProperties() {
        GmailApiRateLimitProperties properties = new GmailApiRateLimitProperties();
        properties.setEnabled(true);
        properties.setFailOpen(true);
        properties.setPerUserCapacityUnits(12000);
        properties.setPerUserRefillUnitsPerMinute(12000);
        properties.setKeyTtl(Duration.ofMinutes(2));
        return properties;
    }
}
