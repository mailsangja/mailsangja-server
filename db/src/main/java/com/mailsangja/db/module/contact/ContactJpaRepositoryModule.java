package com.mailsangja.db.module.contact;

import com.mailsangja.db.entity.contact.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactJpaRepositoryModule extends JpaRepository<Contact, UUID> {
    Optional<Contact> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    List<Contact> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    Page<Contact> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    @Query("""
            SELECT c
            FROM Contact c
            WHERE c.user.id = :userId
              AND c.deletedAt IS NULL
              AND (
                    lower(c.name) LIKE lower(concat('%', :keyword, '%'))
                    OR lower(c.email) LIKE lower(concat('%', :keyword, '%'))
              )
            """)
    List<Contact> findAllByUserIdAndKeywordAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("keyword") String keyword
    );

    @Query("""
            SELECT c
            FROM Contact c
            WHERE c.user.id = :userId
              AND c.deletedAt IS NULL
              AND (
                    lower(c.name) LIKE lower(concat('%', :keyword, '%'))
                    OR lower(c.email) LIKE lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Contact> findAllByUserIdAndKeywordAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Modifying
    @Query(value = """
            INSERT INTO contacts (id, user_id, name, email, created_at, modified_at)
            SELECT id, user_id, name, email, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM unnest(
                CAST(:ids AS text[]),
                CAST(:userIds AS text[]),
                CAST(:names AS text[]),
                CAST(:emails AS text[])
            ) AS contact_data(id, user_id, name, email)
            ON CONFLICT (user_id, (lower(email))) WHERE deleted_at IS NULL DO NOTHING
            """, nativeQuery = true)
    int insertAllIgnoreDuplicateActive(
            @Param("ids") String[] ids,
            @Param("userIds") String[] userIds,
            @Param("names") String[] names,
            @Param("emails") String[] emails
    );

    List<Contact> findAllByUserIdAndEmailInAndDeletedAtIsNull(UUID userId, Collection<String> emails);

    boolean existsByUserIdAndEmailIgnoreCaseAndDeletedAtIsNull(UUID userId, String email);

    @Query("""
            SELECT COUNT(c) > 0
            FROM Contact c
            WHERE c.user.id = :userId
              AND lower(c.email) = lower(:email)
              AND c.id <> :excludeId
              AND c.deletedAt IS NULL
            """)
    boolean existsByUserIdAndEmailIgnoreCaseAndIdNotAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("email") String email,
            @Param("excludeId") UUID excludeId
    );

}
