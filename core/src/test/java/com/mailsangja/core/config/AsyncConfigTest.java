package com.mailsangja.core.config;

import com.mailsangja.core.common.observability.ObservabilitySupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AsyncConfigTest {

    private final TaskDecorator taskDecorator = new AsyncConfig().mdcTaskDecorator();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void mdcTaskDecorator_부모Mdc를작업실행동안복사하고이전값을복원한다() {
        MDC.put(ObservabilitySupport.WORK_ID, "parent-work");
        Runnable decorated = taskDecorator.decorate(() ->
                assertEquals("parent-work", MDC.get(ObservabilitySupport.WORK_ID))
        );

        MDC.clear();
        decorated.run();

        assertNull(MDC.get(ObservabilitySupport.WORK_ID));
    }

    @Test
    void mdcTaskDecorator_작업스레드의기존Mdc를복원한다() {
        MDC.put(ObservabilitySupport.WORK_ID, "parent-work");
        Runnable decorated = taskDecorator.decorate(() ->
                assertEquals("parent-work", MDC.get(ObservabilitySupport.WORK_ID))
        );

        MDC.put(ObservabilitySupport.WORK_ID, "worker-previous");
        decorated.run();

        assertEquals("worker-previous", MDC.get(ObservabilitySupport.WORK_ID));
    }
}
