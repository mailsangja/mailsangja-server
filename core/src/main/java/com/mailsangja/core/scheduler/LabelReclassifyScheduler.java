package com.mailsangja.core.scheduler;

import com.mailsangja.core.config.properties.LabelDebounceProperties;
import com.mailsangja.core.dto.label.LabelReclassifyPendingJob;
import com.mailsangja.core.service.label.LabelReclassifyPendingJobStore;
import com.mailsangja.core.service.label.LabelReclassifyPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelReclassifyScheduler {

    private final LabelReclassifyPendingJobStore pendingJobStore;
    private final LabelReclassifyPublisher labelReclassifyPublisher;
    private final LabelDebounceProperties labelDebounceProperties;

    @Scheduled(fixedDelayString = "${mailsangja.label.debounce.scheduler-fixed-delay-seconds}000")
    public void tick() {
        List<LabelReclassifyPendingJob> jobs =
                pendingJobStore.getPendingJobsReadyToFire(labelDebounceProperties.getDebounceSeconds());

        if (jobs.isEmpty()) {
            return;
        }

        log.info("LabelReclassifyScheduler: {} job(s) ready to fire", jobs.size());

        for (LabelReclassifyPendingJob job : jobs) {
            fireJob(job);
        }
    }

    private void fireJob(LabelReclassifyPendingJob job) {
        UUID userId = job.userId();
        try {
            String jobId = UUID.randomUUID().toString();
            labelReclassifyPublisher.publish(userId, job.labelIds(), jobId);
            pendingJobStore.removePendingJob(userId);
            log.info("Fired label reclassify job: userId={} labelCount={} jobId={}",
                    userId, job.labelIds().size(), jobId);
        } catch (Exception e) {
            log.warn("Failed to fire label reclassify job for userId={}", userId, e);
        }
    }
}
