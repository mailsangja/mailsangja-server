package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageJpaRepositoryModule extends JpaRepository<Message, UUID> {

    Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId);

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.id = :threadId AND m.deletedAt IS NULL ORDER BY m.sentAt ASC")
    List<Message> findAllByThreadIdAndDeletedAtIsNull(@Param("threadId") UUID threadId);

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.id IN :threadIds AND m.deletedAt IS NULL")
    List<Message> findAllByThreadIdInAndDeletedAtIsNull(@Param("threadIds") List<UUID> threadIds);

    // 스레드 상세 조회: INBOUND/OUTBOUND 두 Thread 행의 메시지를 모두 반환 (전체 대화)
    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.mailAccount.id = :mailAccountId AND m.thread.gmailThreadId = :gmailThreadId AND m.deletedAt IS NULL ORDER BY m.sentAt ASC")
    List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @EntityGraph(attributePaths = {"thread", "thread.mailAccount"})
    Optional<Message> findById(UUID id);

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.mailAccount.id = :mailAccountId AND m.thread.gmailThreadId = :gmailThreadId ORDER BY m.sentAt ASC")
    List<Message> findAllByMailAccountIdAndGmailThreadId(
            @Param("mailAccountId") UUID mailAccountId,
            @Param("gmailThreadId") String gmailThreadId
    );

    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.thread.id = :threadId ORDER BY m.sentAt ASC")
    List<Message> findAllByThreadIdIncludingDeleted(@Param("threadId") UUID threadId);
}
