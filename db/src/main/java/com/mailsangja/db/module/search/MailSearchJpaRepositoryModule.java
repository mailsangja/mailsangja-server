package com.mailsangja.db.module.search;

import com.mailsangja.db.entity.mail.Thread;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MailSearchJpaRepositoryModule extends JpaRepository<Thread, UUID> {

    /**
     * 첫 페이지: 마커 없이 한국어 FTS로 매칭되는 스레드 ID를 최신순으로 반환한다.
     * websearch_to_tsquery: 사용자 입력을 웹 검색 형식으로 파싱 (AND/OR/NOT/따옴표 지원).
     */
    @Query(value = """
            SELECT t.id
            FROM threads t
            JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id    = :userId
              AND ma.is_active  = TRUE
              AND ma.deleted_at IS NULL
              AND t.deleted_at  IS NULL
              AND (
                EXISTS (
                  SELECT 1 FROM messages m
                  WHERE m.thread_id  = t.id
                    AND m.deleted_at IS NULL
                    AND (m.search_vector @@ websearch_to_tsquery('korean', :query)
                         OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')
                )
                OR EXISTS (
                  SELECT 1 FROM attachments a
                  JOIN messages m ON a.message_id = m.id
                  WHERE m.thread_id  = t.id
                    AND m.deleted_at IS NULL
                    AND a.deleted_at IS NULL
                    AND (a.filename_vector @@ websearch_to_tsquery('korean', :query)
                         OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')
                )
              )
            ORDER BY t.last_message_at DESC, t.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> searchThreadIdsFirstPage(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("limit") int limit
    );

    /**
     * 다음 페이지: 마커 스레드 이후의 FTS 매칭 스레드 ID를 반환한다.
     */
    @Query(value = """
            SELECT t.id
            FROM threads t
            JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id    = :userId
              AND ma.is_active  = TRUE
              AND ma.deleted_at IS NULL
              AND t.deleted_at  IS NULL
              AND (
                EXISTS (
                  SELECT 1 FROM messages m
                  WHERE m.thread_id  = t.id
                    AND m.deleted_at IS NULL
                    AND (m.search_vector @@ websearch_to_tsquery('korean', :query)
                         OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')
                )
                OR EXISTS (
                  SELECT 1 FROM attachments a
                  JOIN messages m ON a.message_id = m.id
                  WHERE m.thread_id  = t.id
                    AND m.deleted_at IS NULL
                    AND a.deleted_at IS NULL
                    AND (a.filename_vector @@ websearch_to_tsquery('korean', :query)
                         OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')
                )
              )
              AND (
                t.last_message_at < (SELECT t2.last_message_at FROM threads t2
                                     JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                     WHERE t2.id = :markerId AND ma2.user_id = :userId
                                       AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL
                                       AND t2.deleted_at IS NULL)
                OR (
                  t.last_message_at = (SELECT t2.last_message_at FROM threads t2
                                       JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                       WHERE t2.id = :markerId AND ma2.user_id = :userId
                                         AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL
                                         AND t2.deleted_at IS NULL)
                  AND t.id < :markerId
                )
              )
            ORDER BY t.last_message_at DESC, t.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> searchThreadIdsAfterMarker(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("markerId") String markerId,
            @Param("limit") int limit
    );

    @EntityGraph(attributePaths = {"mailAccount"})
    @Query("SELECT t FROM Thread t WHERE t.id IN :ids")
    List<Thread> findAllByIdInWithMailAccount(@Param("ids") List<UUID> ids);

    // -------------------------------------------------------------------------
    // inbox FTS (direction = 'INBOUND', t.deleted_at IS NULL)
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT t.id FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.direction = 'INBOUND'
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            ORDER BY t.last_message_at DESC, t.id DESC LIMIT :limit
            """, nativeQuery = true)
    List<String> searchInboxThreadIdsFirstPage(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT t.id FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.direction = 'INBOUND'
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
              AND (t.last_message_at < (SELECT t2.last_message_at FROM threads t2
                                        JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                        WHERE t2.id = :markerId AND ma2.user_id = :userId
                                          AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL AND t2.deleted_at IS NULL)
                   OR (t.last_message_at = (SELECT t2.last_message_at FROM threads t2
                                            JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                            WHERE t2.id = :markerId AND ma2.user_id = :userId
                                              AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL AND t2.deleted_at IS NULL)
                       AND t.id < :markerId))
            ORDER BY t.last_message_at DESC, t.id DESC LIMIT :limit
            """, nativeQuery = true)
    List<String> searchInboxThreadIdsAfterMarker(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read,
            @Param("markerId") String markerId,
            @Param("limit") int limit
    );

    // -------------------------------------------------------------------------
    // sent FTS (direction = 'OUTBOUND', t.deleted_at IS NULL)
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT t.id FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.direction = 'OUTBOUND'
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            ORDER BY t.last_message_at DESC, t.id DESC LIMIT :limit
            """, nativeQuery = true)
    List<String> searchSentThreadIdsFirstPage(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT t.id FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.direction = 'OUTBOUND'
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
              AND (t.last_message_at < (SELECT t2.last_message_at FROM threads t2
                                        JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                        WHERE t2.id = :markerId AND ma2.user_id = :userId
                                          AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL AND t2.deleted_at IS NULL)
                   OR (t.last_message_at = (SELECT t2.last_message_at FROM threads t2
                                            JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                            WHERE t2.id = :markerId AND ma2.user_id = :userId
                                              AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL AND t2.deleted_at IS NULL)
                       AND t.id < :markerId))
            ORDER BY t.last_message_at DESC, t.id DESC LIMIT :limit
            """, nativeQuery = true)
    List<String> searchSentThreadIdsAfterMarker(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read,
            @Param("markerId") String markerId,
            @Param("limit") int limit
    );

    // -------------------------------------------------------------------------
    // trash FTS (m.deleted_at IS NOT NULL, 메시지 ID 반환)
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT m.id FROM messages m JOIN threads t ON m.thread_id = t.id JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND m.deleted_at IS NOT NULL
              AND ((m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')
                   OR EXISTS (SELECT 1 FROM attachments a WHERE a.message_id = m.id AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR m.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml WHERE ml.message_id = m.id AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            ORDER BY m.deleted_at DESC, m.id DESC LIMIT :limit
            """, nativeQuery = true)
    List<String> searchTrashMessageIdsFirstPage(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT m.id FROM messages m JOIN threads t ON m.thread_id = t.id JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND m.deleted_at IS NOT NULL
              AND ((m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')
                   OR EXISTS (SELECT 1 FROM attachments a WHERE a.message_id = m.id AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR m.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml WHERE ml.message_id = m.id AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
              AND (m.deleted_at < (SELECT mm.deleted_at FROM messages mm
                                    JOIN threads t2 ON mm.thread_id = t2.id
                                    JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                    WHERE mm.id = :markerId AND ma2.user_id = :userId
                                      AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL)
                   OR (m.deleted_at = (SELECT mm.deleted_at FROM messages mm
                                       JOIN threads t2 ON mm.thread_id = t2.id
                                       JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                       WHERE mm.id = :markerId AND ma2.user_id = :userId
                                         AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL)
                       AND m.id < :markerId))
            ORDER BY m.deleted_at DESC, m.id DESC LIMIT :limit
            """, nativeQuery = true)
    List<String> searchTrashMessageIdsAfterMarker(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read,
            @Param("markerId") String markerId,
            @Param("limit") int limit
    );

    // -------------------------------------------------------------------------
    // starred FTS (t.is_star = TRUE, t.deleted_at IS NULL)
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT t.id FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.is_star = TRUE
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            ORDER BY t.last_message_at DESC, t.id DESC LIMIT :limit
            """, nativeQuery = true)
    List<String> searchStarredThreadIdsFirstPage(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT t.id FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.is_star = TRUE
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
              AND (t.last_message_at < (SELECT t2.last_message_at FROM threads t2
                                        JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                        WHERE t2.id = :markerId AND ma2.user_id = :userId
                                          AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL AND t2.deleted_at IS NULL)
                   OR (t.last_message_at = (SELECT t2.last_message_at FROM threads t2
                                            JOIN mail_accounts ma2 ON t2.mail_account_id = ma2.id
                                            WHERE t2.id = :markerId AND ma2.user_id = :userId
                                              AND ma2.is_active = TRUE AND ma2.deleted_at IS NULL AND t2.deleted_at IS NULL)
                       AND t.id < :markerId))
            ORDER BY t.last_message_at DESC, t.id DESC LIMIT :limit
            """, nativeQuery = true)
    List<String> searchStarredThreadIdsAfterMarker(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read,
            @Param("markerId") String markerId,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT COUNT(*) FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.is_star = TRUE
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            """, nativeQuery = true)
    Long countStarredThreads(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read
    );

    @Query(value = """
            SELECT COUNT(*) FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.is_star = TRUE AND t.is_read = FALSE
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            """, nativeQuery = true)
    Long countUnreadStarredThreads(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty
    );

    // -------------------------------------------------------------------------
    // COUNT queries (inbox / sent / trash)
    // -------------------------------------------------------------------------

    @Query(value = """
            SELECT COUNT(*) FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.direction = 'INBOUND'
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            """, nativeQuery = true)
    Long countInboxThreads(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read
    );

    @Query(value = """
            SELECT COUNT(*) FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.direction = 'INBOUND' AND t.is_read = FALSE
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            """, nativeQuery = true)
    Long countUnreadInboxThreads(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty
    );

    @Query(value = """
            SELECT COUNT(*) FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.direction = 'OUTBOUND'
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR t.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            """, nativeQuery = true)
    Long countSentThreads(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read
    );

    @Query(value = """
            SELECT COUNT(*) FROM threads t JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND t.deleted_at IS NULL AND t.direction = 'OUTBOUND' AND t.is_read = FALSE
              AND (EXISTS (SELECT 1 FROM messages m WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND (m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'))
                   OR EXISTS (SELECT 1 FROM attachments a JOIN messages m ON a.message_id = m.id WHERE m.thread_id = t.id AND m.deleted_at IS NULL AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml JOIN messages m2 ON ml.message_id = m2.id WHERE m2.thread_id = t.id AND m2.deleted_at IS NULL AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            """, nativeQuery = true)
    Long countUnreadSentThreads(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty
    );

    @Query(value = """
            SELECT COUNT(*) FROM messages m JOIN threads t ON m.thread_id = t.id JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND m.deleted_at IS NOT NULL
              AND ((m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')
                   OR EXISTS (SELECT 1 FROM attachments a WHERE a.message_id = m.id AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:read IS NULL OR m.is_read = :read)
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml WHERE ml.message_id = m.id AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            """, nativeQuery = true)
    Long countTrashMessages(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty,
            @Param("read") Boolean read
    );

    @Query(value = """
            SELECT COUNT(*) FROM messages m JOIN threads t ON m.thread_id = t.id JOIN mail_accounts ma ON t.mail_account_id = ma.id
            WHERE ma.user_id = :userId AND ma.is_active = TRUE AND ma.deleted_at IS NULL
              AND m.deleted_at IS NOT NULL AND m.is_read = FALSE
              AND ((m.search_vector @@ websearch_to_tsquery('korean', :query) OR m.search_text LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')
                   OR EXISTS (SELECT 1 FROM attachments a WHERE a.message_id = m.id AND a.deleted_at IS NULL AND (a.filename_vector @@ websearch_to_tsquery('korean', :query) OR lower(a.filename) LIKE '%' || replace(replace(replace(lower(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!')))
              AND (:labelsEmpty = TRUE OR EXISTS (SELECT 1 FROM message_labels ml WHERE ml.message_id = m.id AND ml.label_id IN (:labelIds) AND ml.deleted_at IS NULL))
            """, nativeQuery = true)
    Long countUnreadTrashMessages(
            @Param("userId") String userId,
            @Param("query") String query,
            @Param("labelIds") List<String> labelIds,
            @Param("labelsEmpty") boolean labelsEmpty
    );
}
