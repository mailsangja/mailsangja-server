package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.Direction;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageJpaRepositoryModuleTest {

    @Test
    void sensitiveLabelsExcludedJpqlQueriesContainNotExistsFilter() throws NoSuchMethodException {
        assertAll(
                () -> assertJpqlSensitiveLabelFilter("findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndSensitiveLabelsExcluded", UUID.class, String.class),
                () -> assertJpqlSensitiveLabelFilter("findByIdIncludingDeletedAndSensitiveLabelsExcluded", UUID.class),
                () -> assertJpqlSensitiveLabelFilter("findRecentByUserIdAndMailAccountIdAndDirection", UUID.class, UUID.class, Direction.class, Pageable.class),
                () -> assertJpqlSensitiveLabelFilter("findRecentByUserIdAndDirection", UUID.class, Direction.class, Pageable.class),
                () -> assertJpqlSensitiveLabelFilter("findActiveByIdIn", List.class),
                () -> assertJpqlSensitiveLabelFilter("findThreadContextByReplyMessageId", UUID.class)
        );
    }

    @Test
    void sensitiveLabelsExcludedNativeQueriesContainNotExistsFilter() throws NoSuchMethodException {
        assertAll(
                () -> assertNativeSensitiveLabelFilter("findWrittenByUserIdAndMailAccountIdAndHint", String.class, String.class, String.class, Pageable.class),
                () -> assertNativeSensitiveLabelFilter("findRecipientHistoryByUserIdAndMailAccountIdAndHint", String.class, String.class, String.class, Pageable.class)
        );
    }

    private void assertJpqlSensitiveLabelFilter(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        String query = queryOf(methodName, parameterTypes);
        String normalized = normalize(query);

        assertAll(
                () -> assertTrue(normalized.contains("not exists"), methodName + " must exclude sensitive labels with NOT EXISTS"),
                () -> assertTrue(normalized.contains("from messagelabel ml"), methodName + " must inspect MessageLabel"),
                () -> assertTrue(normalized.contains("ml.message.id = m.id"), methodName + " must correlate MessageLabel to Message"),
                () -> assertTrue(normalized.contains("ml.deletedat is null"), methodName + " must ignore deleted MessageLabel rows"),
                () -> assertTrue(normalized.contains("ml.label.deletedat is null"), methodName + " must ignore deleted Label rows"),
                () -> assertTrue(normalized.contains("ml.label.issensitive = true"), methodName + " must exclude sensitive labels")
        );
    }

    private void assertNativeSensitiveLabelFilter(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        String query = queryOf(methodName, parameterTypes);
        String normalized = normalize(query);

        assertAll(
                () -> assertTrue(normalized.contains("not exists"), methodName + " must exclude sensitive labels with NOT EXISTS"),
                () -> assertTrue(normalized.contains("from message_labels ml"), methodName + " must inspect message_labels"),
                () -> assertTrue(normalized.contains("join labels l on l.id = ml.label_id"), methodName + " must join labels"),
                () -> assertTrue(normalized.contains("ml.message_id = m.id"), methodName + " must correlate message_labels to messages"),
                () -> assertTrue(normalized.contains("ml.deleted_at is null"), methodName + " must ignore deleted message_labels rows"),
                () -> assertTrue(normalized.contains("l.deleted_at is null"), methodName + " must ignore deleted labels rows"),
                () -> assertTrue(normalized.contains("l.is_sensitive = true"), methodName + " must exclude sensitive labels")
        );
    }

    private String queryOf(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = MessageJpaRepositoryModule.class.getDeclaredMethod(methodName, parameterTypes);
        Query query = method.getAnnotation(Query.class);

        assertNotNull(query, methodName + " must declare @Query");
        return query.value();
    }

    private String normalize(String query) {
        return query.replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
