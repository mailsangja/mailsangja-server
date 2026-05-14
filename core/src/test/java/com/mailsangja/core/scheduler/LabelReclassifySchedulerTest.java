package com.mailsangja.core.scheduler;

import com.mailsangja.core.config.properties.LabelDebounceProperties;
import com.mailsangja.core.dto.label.LabelReclassifyPendingJob;
import com.mailsangja.core.service.label.LabelReclassifyPendingJobStore;
import com.mailsangja.core.service.label.LabelReclassifyPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelReclassifySchedulerTest {

    @Mock
    private LabelReclassifyPendingJobStore pendingJobStore;

    @Mock
    private LabelReclassifyPublisher labelReclassifyPublisher;

    @Mock
    private LabelDebounceProperties labelDebounceProperties;

    @InjectMocks
    private LabelReclassifyScheduler scheduler;

    @Test
    void 스케줄러실행_발동준비된잡없으면_발행안됨() {
        when(labelDebounceProperties.getDebounceSeconds()).thenReturn(60);
        when(pendingJobStore.getPendingJobsReadyToFire(60)).thenReturn(List.of());

        scheduler.tick();

        verify(labelReclassifyPublisher, never()).publish(any(), any(), any());
        verify(pendingJobStore, never()).removePendingJob(any());
    }

    @Test
    void 스케줄러실행_준비된잡있으면_발행후PendingJob제거() {
        UUID userId = UUID.randomUUID();
        Set<UUID> labelIds = Set.of(UUID.randomUUID());
        LabelReclassifyPendingJob job = new LabelReclassifyPendingJob(userId, labelIds);
        when(labelDebounceProperties.getDebounceSeconds()).thenReturn(60);
        when(pendingJobStore.getPendingJobsReadyToFire(60)).thenReturn(List.of(job));

        scheduler.tick();

        verify(labelReclassifyPublisher).publish(eq(userId), eq(labelIds), anyString());
        verify(pendingJobStore).removePendingJob(userId);
    }

    @Test
    void 스케줄러실행_여러잡있으면_모두발행() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Set<UUID> labelIds1 = Set.of(UUID.randomUUID());
        Set<UUID> labelIds2 = Set.of(UUID.randomUUID());
        LabelReclassifyPendingJob job1 = new LabelReclassifyPendingJob(userId1, labelIds1);
        LabelReclassifyPendingJob job2 = new LabelReclassifyPendingJob(userId2, labelIds2);
        when(labelDebounceProperties.getDebounceSeconds()).thenReturn(60);
        when(pendingJobStore.getPendingJobsReadyToFire(60)).thenReturn(List.of(job1, job2));

        scheduler.tick();

        verify(labelReclassifyPublisher).publish(eq(userId1), eq(labelIds1), anyString());
        verify(labelReclassifyPublisher).publish(eq(userId2), eq(labelIds2), anyString());
        verify(pendingJobStore).removePendingJob(userId1);
        verify(pendingJobStore).removePendingJob(userId2);
    }

    @Test
    void 스케줄러실행_발행실패시_예외전파안되고PendingJob유지() {
        UUID userId = UUID.randomUUID();
        LabelReclassifyPendingJob job = new LabelReclassifyPendingJob(userId, Set.of(UUID.randomUUID()));
        when(labelDebounceProperties.getDebounceSeconds()).thenReturn(60);
        when(pendingJobStore.getPendingJobsReadyToFire(60)).thenReturn(List.of(job));
        doThrow(new RuntimeException("connection failed"))
                .when(labelReclassifyPublisher).publish(any(), any(), any());

        assertDoesNotThrow(() -> scheduler.tick());
        verify(pendingJobStore, never()).removePendingJob(userId);
    }
}
