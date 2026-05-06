package com.mailsangja.db.port;

import com.mailsangja.db.entity.contact.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactRepositoryPort {
    Contact save(Contact contact);

    List<Contact> saveAll(List<Contact> contacts);

    int saveAllIgnoreDuplicateActive(List<Contact> contacts);

    Optional<Contact> findByIdAndUserIdAndDeletedAtIsNull(UUID contactId, UUID userId);

    Page<Contact> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Page<Contact> findAllByUserIdAndKeywordAndDeletedAtIsNull(UUID userId, String keyword, Pageable pageable);

    List<Contact> findAllByUserIdAndEmailInAndDeletedAtIsNull(UUID userId, List<String> emails);

    boolean existsByUserIdAndEmailIgnoreCaseAndDeletedAtIsNull(UUID userId, String email);

    boolean existsByUserIdAndEmailIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID userId, String email, UUID excludeId);
}
