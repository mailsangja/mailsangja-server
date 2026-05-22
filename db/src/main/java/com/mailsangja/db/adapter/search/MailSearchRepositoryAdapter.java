package com.mailsangja.db.adapter.search;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.module.mail.MessageJpaRepositoryModule;
import com.mailsangja.db.module.search.MailSearchJpaRepositoryModule;
import com.mailsangja.db.port.MailSearchRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MailSearchRepositoryAdapter implements MailSearchRepositoryPort {

    private final MailSearchJpaRepositoryModule mailSearchJpaRepositoryModule;
    private final MessageJpaRepositoryModule messageJpaRepositoryModule;

    @Override
    public Slice<Thread> searchThreads(UUID userId, String query, UUID markerId, Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        String userIdStr = userId.toString();

        List<String> rawIds = (markerId == null)
                ? mailSearchJpaRepositoryModule.searchThreadIdsFirstPage(userIdStr, query, limit)
                : mailSearchJpaRepositoryModule.searchThreadIdsAfterMarker(userIdStr, query, markerId.toString(), limit);

        if (rawIds.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        boolean hasNext = rawIds.size() > pageable.getPageSize();
        List<String> pageRawIds = hasNext ? rawIds.subList(0, pageable.getPageSize()) : rawIds;
        List<UUID> pageIds = pageRawIds.stream().map(UUID::fromString).toList();

        Map<UUID, Thread> threadById = mailSearchJpaRepositoryModule
                .findAllByIdInWithMailAccount(pageIds)
                .stream()
                .collect(Collectors.toMap(Thread::getId, Function.identity()));

        List<Thread> ordered = pageIds.stream()
                .map(id -> {
                    Thread thread = threadById.get(id);
                    if (thread == null) {
                        log.warn("Search result thread not found in DB: threadId={}", id);
                    }
                    return thread;
                })
                .filter(Objects::nonNull)
                .toList();

        return new SliceImpl<>(ordered, pageable, hasNext);
    }

    @Override
    public Slice<Thread> searchInboxThreads(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        String userIdStr = userId.toString();
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();

        List<String> rawIds = (markerId == null)
                ? mailSearchJpaRepositoryModule.searchInboxThreadIdsFirstPage(userIdStr, query, effectiveLabelIds, labelsEmpty, read, limit)
                : mailSearchJpaRepositoryModule.searchInboxThreadIdsAfterMarker(userIdStr, query, effectiveLabelIds, labelsEmpty, read, markerId.toString(), limit);

        if (rawIds.isEmpty()) return new SliceImpl<>(Collections.emptyList(), pageable, false);

        boolean hasNext = rawIds.size() > pageable.getPageSize();
        List<UUID> pageIds = (hasNext ? rawIds.subList(0, pageable.getPageSize()) : rawIds)
                .stream().map(UUID::fromString).toList();

        Map<UUID, Thread> threadById = mailSearchJpaRepositoryModule.findAllByIdInWithMailAccount(pageIds)
                .stream().collect(Collectors.toMap(Thread::getId, Function.identity()));
        List<Thread> ordered = pageIds.stream().map(threadById::get).filter(Objects::nonNull).toList();
        return new SliceImpl<>(ordered, pageable, hasNext);
    }

    @Override
    public Slice<Thread> searchSentThreads(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        String userIdStr = userId.toString();
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();

        List<String> rawIds = (markerId == null)
                ? mailSearchJpaRepositoryModule.searchSentThreadIdsFirstPage(userIdStr, query, effectiveLabelIds, labelsEmpty, read, limit)
                : mailSearchJpaRepositoryModule.searchSentThreadIdsAfterMarker(userIdStr, query, effectiveLabelIds, labelsEmpty, read, markerId.toString(), limit);

        if (rawIds.isEmpty()) return new SliceImpl<>(Collections.emptyList(), pageable, false);

        boolean hasNext = rawIds.size() > pageable.getPageSize();
        List<UUID> pageIds = (hasNext ? rawIds.subList(0, pageable.getPageSize()) : rawIds)
                .stream().map(UUID::fromString).toList();

        Map<UUID, Thread> threadById = mailSearchJpaRepositoryModule.findAllByIdInWithMailAccount(pageIds)
                .stream().collect(Collectors.toMap(Thread::getId, Function.identity()));
        List<Thread> ordered = pageIds.stream().map(threadById::get).filter(Objects::nonNull).toList();
        return new SliceImpl<>(ordered, pageable, hasNext);
    }

    @Override
    public Slice<Message> searchTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read, UUID markerId, Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        String userIdStr = userId.toString();
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();

        List<String> rawIds = (markerId == null)
                ? mailSearchJpaRepositoryModule.searchTrashMessageIdsFirstPage(userIdStr, query, effectiveLabelIds, labelsEmpty, read, limit)
                : mailSearchJpaRepositoryModule.searchTrashMessageIdsAfterMarker(userIdStr, query, effectiveLabelIds, labelsEmpty, read, markerId.toString(), limit);

        if (rawIds.isEmpty()) return new SliceImpl<>(Collections.emptyList(), pageable, false);

        boolean hasNext = rawIds.size() > pageable.getPageSize();
        List<UUID> pageIds = (hasNext ? rawIds.subList(0, pageable.getPageSize()) : rawIds)
                .stream().map(UUID::fromString).toList();

        Map<UUID, Message> messageById = messageJpaRepositoryModule.findAllByIdInWithThread(pageIds)
                .stream().collect(Collectors.toMap(Message::getId, Function.identity()));
        List<Message> ordered = pageIds.stream().map(messageById::get).filter(Objects::nonNull).toList();
        return new SliceImpl<>(ordered, pageable, hasNext);
    }

    @Override
    public long countInboxThreads(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();
        Long result = mailSearchJpaRepositoryModule.countInboxThreads(userId.toString(), query, effectiveLabelIds, labelsEmpty, read);
        return result != null ? result : 0L;
    }

    @Override
    public long countUnreadInboxThreads(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        if (Boolean.TRUE.equals(read)) {
            return 0L;
        }
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();
        Long result = mailSearchJpaRepositoryModule.countUnreadInboxThreads(userId.toString(), query, effectiveLabelIds, labelsEmpty);
        return result != null ? result : 0L;
    }

    @Override
    public long countSentThreads(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();
        Long result = mailSearchJpaRepositoryModule.countSentThreads(userId.toString(), query, effectiveLabelIds, labelsEmpty, read);
        return result != null ? result : 0L;
    }

    @Override
    public long countUnreadSentThreads(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        if (Boolean.TRUE.equals(read)) {
            return 0L;
        }
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();
        Long result = mailSearchJpaRepositoryModule.countUnreadSentThreads(userId.toString(), query, effectiveLabelIds, labelsEmpty);
        return result != null ? result : 0L;
    }

    @Override
    public long countTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();
        Long result = mailSearchJpaRepositoryModule.countTrashMessages(userId.toString(), query, effectiveLabelIds, labelsEmpty, read);
        return result != null ? result : 0L;
    }

    @Override
    public long countUnreadTrashMessages(UUID userId, String query, List<UUID> labelIds, Boolean read) {
        if (Boolean.TRUE.equals(read)) {
            return 0L;
        }
        List<String> effectiveLabelIds = toEffectiveLabelIds(labelIds);
        boolean labelsEmpty = labelIds == null || labelIds.isEmpty();
        Long result = mailSearchJpaRepositoryModule.countUnreadTrashMessages(userId.toString(), query, effectiveLabelIds, labelsEmpty);
        return result != null ? result : 0L;
    }

    private List<String> toEffectiveLabelIds(List<UUID> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return List.of("00000000-0000-0000-0000-000000000000");
        }
        return labelIds.stream().map(UUID::toString).toList();
    }
}
