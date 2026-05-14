package com.mailsangja.core.service.label;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.config.properties.LabelDebounceProperties;
import com.mailsangja.core.dto.label.LabelReclassifyPendingJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelReclassifyPendingJobStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private LabelDebounceProperties labelDebounceProperties;

    @InjectMocks
    private LabelReclassifyPendingJobStore store;

    // --- checkRateLimit ---

    @Test
    void RateLimit확인_count가null이면_예외없음() {
        when(labelDebounceProperties.getRateLimitWindowSeconds()).thenReturn(60);
        doReturn(null).when(stringRedisTemplate).execute(any(RedisScript.class), anyList(), anyString());

        assertDoesNotThrow(() -> store.checkRateLimit(UUID.randomUUID()));
    }

    @Test
    void RateLimit확인_한도미만이면_예외없음() {
        when(labelDebounceProperties.getRateLimitWindowSeconds()).thenReturn(60);
        when(labelDebounceProperties.getRateLimitMaxRequests()).thenReturn(20);
        doReturn(1L).when(stringRedisTemplate).execute(any(RedisScript.class), anyList(), anyString());

        assertDoesNotThrow(() -> store.checkRateLimit(UUID.randomUUID()));
    }

    @Test
    void RateLimit확인_한도정확히이면_예외없음() {
        when(labelDebounceProperties.getRateLimitWindowSeconds()).thenReturn(60);
        when(labelDebounceProperties.getRateLimitMaxRequests()).thenReturn(20);
        doReturn(20L).when(stringRedisTemplate).execute(any(RedisScript.class), anyList(), anyString());

        assertDoesNotThrow(() -> store.checkRateLimit(UUID.randomUUID()));
    }

    @Test
    void RateLimit확인_한도초과시_RateLimited예외발생() {
        UUID userId = UUID.randomUUID();
        String rateLimitKey = "LabelReclassify:rateLimit:" + userId;
        when(labelDebounceProperties.getRateLimitWindowSeconds()).thenReturn(60);
        when(labelDebounceProperties.getRateLimitMaxRequests()).thenReturn(20);
        doReturn(21L).when(stringRedisTemplate).execute(any(RedisScript.class), anyList(), anyString());
        when(stringRedisTemplate.getExpire(rateLimitKey)).thenReturn(45L);

        LabelException exception = assertThrows(LabelException.class, () -> store.checkRateLimit(userId));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_RATE_LIMITED, exception.getErrorCode());
        assertEquals(45L, exception.getRetryAfterSeconds());
    }

    @Test
    void RateLimit확인_한도초과시TTL음수면_윈도우초를retryAfter로사용() {
        UUID userId = UUID.randomUUID();
        String rateLimitKey = "LabelReclassify:rateLimit:" + userId;
        when(labelDebounceProperties.getRateLimitWindowSeconds()).thenReturn(60);
        when(labelDebounceProperties.getRateLimitMaxRequests()).thenReturn(20);
        doReturn(21L).when(stringRedisTemplate).execute(any(RedisScript.class), anyList(), anyString());
        when(stringRedisTemplate.getExpire(rateLimitKey)).thenReturn(-1L);

        LabelException exception = assertThrows(LabelException.class, () -> store.checkRateLimit(userId));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_RATE_LIMITED, exception.getErrorCode());
        assertEquals(60L, exception.getRetryAfterSeconds());
    }

    // --- mergePendingJob ---

    @Test
    @SuppressWarnings("unchecked")
    void PendingJob병합_기존데이터없으면_라벨ID단독저장() {
        UUID userId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        String key = "LabelReclassify:pending:" + userId;
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(key, "labelIds")).thenReturn(null);

        store.mergePendingJob(userId, labelId);

        verify(hashOps).put(key, "labelIds", labelId.toString());
        verify(hashOps).put(eq(key), eq("lastUpdatedAt"), anyString());
        verify(stringRedisTemplate).expire(key, Duration.ofHours(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void PendingJob병합_기존데이터있으면_새라벨ID추가() {
        UUID userId = UUID.randomUUID();
        UUID existingLabelId = UUID.randomUUID();
        UUID newLabelId = UUID.randomUUID();
        String key = "LabelReclassify:pending:" + userId;
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(key, "labelIds")).thenReturn(existingLabelId.toString());

        store.mergePendingJob(userId, newLabelId);

        verify(hashOps).put(eq(key), eq("labelIds"), argThat(s -> {
            String v = (String) s;
            return v.contains(existingLabelId.toString()) && v.contains(newLabelId.toString());
        }));
    }

    @Test
    @SuppressWarnings("unchecked")
    void PendingJob병합_중복라벨ID는_중복제거됨() {
        UUID userId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        String key = "LabelReclassify:pending:" + userId;
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(key, "labelIds")).thenReturn(labelId.toString());

        store.mergePendingJob(userId, labelId);

        verify(hashOps).put(key, "labelIds", labelId.toString());
    }

    // --- getPendingJobsReadyToFire ---

    @Test
    @SuppressWarnings("unchecked")
    void 발동준비된PendingJob조회_저장소비어있으면_빈목록반환() {
        Cursor<String> cursor = mock(Cursor.class);
        when(stringRedisTemplate.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

        List<LabelReclassifyPendingJob> result = store.getPendingJobsReadyToFire(60);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 발동준비된PendingJob조회_Debounce경과한잡은_목록에포함() {
        UUID userId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        String key = "LabelReclassify:pending:" + userId;
        long oldEnoughMs = Instant.now().toEpochMilli() - 120_000L;

        Cursor<String> cursor = mock(Cursor.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(key, "lastUpdatedAt")).thenReturn(String.valueOf(oldEnoughMs));
        when(hashOps.get(key, "labelIds")).thenReturn(labelId.toString());

        List<LabelReclassifyPendingJob> result = store.getPendingJobsReadyToFire(60);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).userId());
        assertEquals(Set.of(labelId), result.get(0).labelIds());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 발동준비된PendingJob조회_Debounce미경과한잡은_목록에서제외() {
        UUID userId = UUID.randomUUID();
        String key = "LabelReclassify:pending:" + userId;
        long tooRecentMs = Instant.now().toEpochMilli() - 10_000L;

        Cursor<String> cursor = mock(Cursor.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(key, "lastUpdatedAt")).thenReturn(String.valueOf(tooRecentMs));

        List<LabelReclassifyPendingJob> result = store.getPendingJobsReadyToFire(60);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 발동준비된PendingJob조회_lastUpdatedAt없으면_해당키건너뜀() {
        UUID userId = UUID.randomUUID();
        String key = "LabelReclassify:pending:" + userId;

        Cursor<String> cursor = mock(Cursor.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(stringRedisTemplate.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(key, "lastUpdatedAt")).thenReturn(null);

        List<LabelReclassifyPendingJob> result = store.getPendingJobsReadyToFire(60);

        assertTrue(result.isEmpty());
        verify(hashOps, never()).get(key, "labelIds");
    }

    // --- removePendingJob ---

    @Test
    void PendingJob제거_해당키삭제() {
        UUID userId = UUID.randomUUID();

        store.removePendingJob(userId);

        verify(stringRedisTemplate).delete("LabelReclassify:pending:" + userId);
    }
}
