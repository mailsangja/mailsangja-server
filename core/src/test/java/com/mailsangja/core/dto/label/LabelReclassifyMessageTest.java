package com.mailsangja.core.dto.label;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LabelReclassifyMessageTest {

    @Test
    void constructor_validInput_makesDefensiveCopies() {
        UUID userId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Set<UUID> labelIds = new HashSet<>(Set.of(labelId));
        List<UUID> threadIds = new ArrayList<>(List.of(threadId));

        LabelReclassifyMessage message = new LabelReclassifyMessage(userId, labelIds, threadIds, "job-1");

        labelIds.clear();
        threadIds.clear();

        assertEquals(Set.of(labelId), message.labelIds());
        assertEquals(List.of(threadId), message.threadIds());
        assertThrows(UnsupportedOperationException.class, () -> message.labelIds().add(UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> message.threadIds().add(UUID.randomUUID()));
    }

    @Test
    void constructor_userIdIsNull_throwsInvalidRequest() {
        LabelException exception = assertThrows(LabelException.class,
                () -> new LabelReclassifyMessage(null, Set.of(UUID.randomUUID()), List.of(UUID.randomUUID()), "job-1"));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void constructor_labelIdsIsEmpty_throwsInvalidRequest() {
        LabelException exception = assertThrows(LabelException.class,
                () -> new LabelReclassifyMessage(UUID.randomUUID(), Set.of(), List.of(UUID.randomUUID()), "job-1"));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void constructor_labelIdsContainsNull_throwsInvalidRequest() {
        Set<UUID> labelIds = new HashSet<>();
        labelIds.add(null);

        LabelException exception = assertThrows(LabelException.class,
                () -> new LabelReclassifyMessage(UUID.randomUUID(), labelIds, List.of(UUID.randomUUID()), "job-1"));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void constructor_threadIdsIsEmpty_throwsInvalidRequest() {
        LabelException exception = assertThrows(LabelException.class,
                () -> new LabelReclassifyMessage(UUID.randomUUID(), Set.of(UUID.randomUUID()), List.of(), "job-1"));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void constructor_threadIdsContainsNull_throwsInvalidRequest() {
        List<UUID> threadIds = new ArrayList<>();
        threadIds.add(null);

        LabelException exception = assertThrows(LabelException.class,
                () -> new LabelReclassifyMessage(UUID.randomUUID(), Set.of(UUID.randomUUID()), threadIds, "job-1"));

        assertEquals(LabelErrorCode.LABEL_RECLASSIFY_INVALID_REQUEST, exception.getErrorCode());
    }
}
