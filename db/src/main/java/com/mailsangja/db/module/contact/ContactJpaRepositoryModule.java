package com.mailsangja.db.module.contact;

import com.mailsangja.db.entity.contact.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
