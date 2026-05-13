package com.mailsangja.core.service.label;

import com.mailsangja.core.config.properties.LabelDebounceProperties;
import com.mailsangja.core.dto.label.LabelReclassifyPendingJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelReclassifyPendingJobStore {

    private static final String PENDING_KEY_PREFIX = "LabelReclassify:pending:";
    private static final String RATE_LIMIT_KEY_PREFIX = "LabelReclassify:rateLimit:";
    private static final String FIELD_LABEL_IDS = "labelIds";
    private static final String FIELD_LAST_UPDATED_AT = "lastUpdatedAt";
    private static final Duration PENDING_TTL = Duration.ofHours(1);

    private final StringRedisTemplate stringRedisTemplate;
    private final LabelDebounceProperties labelDebounceProperties;

    /**
     * Rate Limit 체크. 초과 시 LabelException(RATE_LIMITED) throw.
     */
    public void checkRateLimit(UUID userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            return;
        }
        if (count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(labelDebounceProperties.getRateLimitWindowSeconds()));
        }
        if (count > labelDebounceProperties.getRateLimitMaxRequests()) {
            Long ttl = stringRedisTemplate.getExpire(key);
            long retryAfter = (ttl != null && ttl > 0) ? ttl : labelDebounceProperties.getRateLimitWindowSeconds();
            throw new com.mailsangja.core.common.exception.label.LabelException(
                    com.mailsangja.core.common.exception.label.LabelErrorCode.LABEL_RECLASSIFY_RATE_LIMITED,
                    retryAfter
            );
        }
    }

    /**
     * Pending Job에 labelId를 병합하고 lastUpdatedAt을 갱신.
     */
    public void mergePendingJob(UUID userId, UUID labelId) {
        String key = PENDING_KEY_PREFIX + userId;

        String existing = (String) stringRedisTemplate.opsForHash().get(key, FIELD_LABEL_IDS);
        Set<String> labelIdSet = new HashSet<>();
        if (existing != null && !existing.isBlank()) {
            labelIdSet.addAll(Arrays.asList(existing.split(",")));
        }
        labelIdSet.add(labelId.toString());

        String merged = String.join(",", labelIdSet);
        String nowMs = String.valueOf(Instant.now().toEpochMilli());

        stringRedisTemplate.opsForHash().put(key, FIELD_LABEL_IDS, merged);
        stringRedisTemplate.opsForHash().put(key, FIELD_LAST_UPDATED_AT, nowMs);
        stringRedisTemplate.expire(key, PENDING_TTL);
    }

    /**
     * debounceSeconds 이상 업데이트가 없는 Pending Job 목록 반환.
     */
    public List<LabelReclassifyPendingJob> getPendingJobsReadyToFire(int debounceSeconds) {
        long threshold = Instant.now().toEpochMilli() - (long) debounceSeconds * 1000;
        List<LabelReclassifyPendingJob> result = new ArrayList<>();

        ScanOptions options = ScanOptions.scanOptions()
                .match(PENDING_KEY_PREFIX + "*")
                .count(100)
                .build();

        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                processKey(key, threshold, result);
            }
        } catch (Exception e) {
            log.warn("Error scanning pending label reclassify jobs", e);
        }

        return result;
    }

    private void processKey(String key, long threshold, List<LabelReclassifyPendingJob> result) {
        try {
            Object lastUpdatedAtObj = stringRedisTemplate.opsForHash().get(key, FIELD_LAST_UPDATED_AT);
            if (lastUpdatedAtObj == null) {
                return;
            }
            long lastUpdatedAt = Long.parseLong(lastUpdatedAtObj.toString());
            if (lastUpdatedAt > threshold) {
                return;
            }

            Object labelIdsObj = stringRedisTemplate.opsForHash().get(key, FIELD_LABEL_IDS);
            if (labelIdsObj == null || labelIdsObj.toString().isBlank()) {
                return;
            }

            String userIdStr = key.substring(PENDING_KEY_PREFIX.length());
            UUID userId = UUID.fromString(userIdStr);

            Set<UUID> labelIds = Arrays.stream(labelIdsObj.toString().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());

            if (labelIds.isEmpty()) {
                return;
            }

            result.add(new LabelReclassifyPendingJob(userId, labelIds));
        } catch (Exception e) {
            log.warn("Failed to parse pending job for key={}", key, e);
        }
    }

    /**
     * Pending Job 제거.
     */
    public void removePendingJob(UUID userId) {
        stringRedisTemplate.delete(PENDING_KEY_PREFIX + userId);
    }
}
