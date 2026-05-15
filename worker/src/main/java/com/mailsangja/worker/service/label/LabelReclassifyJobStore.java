package com.mailsangja.worker.service.label;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LabelReclassifyJobStore {

    private static final String LATEST_JOB_ID_KEY_PREFIX = "LabelReclassify:latestJobId:";

    private final StringRedisTemplate stringRedisTemplate;

    public String getLatestJobId(UUID labelId) {
        return stringRedisTemplate.opsForValue().get(LATEST_JOB_ID_KEY_PREFIX + labelId);
    }
}
