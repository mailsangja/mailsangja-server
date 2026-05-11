package com.mailsangja.db.adapter.contact;

import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.module.contact.ContactBulkInsertModule;
import com.mailsangja.db.module.contact.ContactJpaRepositoryModule;
import com.mailsangja.db.port.ContactRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ContactRepositoryAdapter implements ContactRepositoryPort {

    private final ContactJpaRepositoryModule contactJpaRepositoryModule;
    private final ContactBulkInsertModule contactBulkInsertModule;

    @Override
    public Contact save(Contact contact) {
        return contactJpaRepositoryModule.save(contact);
    }

    @Override
    public List<Contact> saveAll(List<Contact> contacts) {
        return contactJpaRepositoryModule.saveAll(contacts);
    }

    @Override
    public int saveAllIgnoreDuplicateActive(List<Contact> contacts) {
        if (contacts.isEmpty()) {
            return 0;
        }

        String[] ids = new String[contacts.size()];
        String[] userIds = new String[contacts.size()];
        String[] names = new String[contacts.size()];
        String[] emails = new String[contacts.size()];

        for (int index = 0; index < contacts.size(); index++) {
            Contact contact = contacts.get(index);
            ids[index] = UUID.randomUUID().toString();
            userIds[index] = contact.getUser().getId().toString();
            names[index] = contact.getName();
            emails[index] = contact.getEmail();
        }

        return contactBulkInsertModule.insertAllIgnoreDuplicateActive(ids, userIds, names, emails);
    }

    @Override
    public Optional<Contact> findByIdAndUserIdAndDeletedAtIsNull(UUID contactId, UUID userId) {
        return contactJpaRepositoryModule.findByIdAndUserIdAndDeletedAtIsNull(contactId, userId);
    }

    @Override
    public List<Contact> findAllByUserIdAndDeletedAtIsNull(UUID userId) {
        return contactJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public Page<Contact> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable) {
        return contactJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNull(userId, pageable);
    }

    @Override
    public List<Contact> findAllByUserIdAndKeywordAndDeletedAtIsNull(UUID userId, String keyword) {
        return contactJpaRepositoryModule.findAllByUserIdAndKeywordAndDeletedAtIsNull(userId, keyword);
    }

    @Override
    public Page<Contact> findAllByUserIdAndKeywordAndDeletedAtIsNull(UUID userId, String keyword, Pageable pageable) {
        return contactJpaRepositoryModule.findAllByUserIdAndKeywordAndDeletedAtIsNull(userId, keyword, pageable);
    }

    @Override
    public List<Contact> findAllByUserIdAndEmailInAndDeletedAtIsNull(UUID userId, List<String> emails) {
        return contactJpaRepositoryModule.findAllByUserIdAndEmailInAndDeletedAtIsNull(userId, emails);
    }

    @Override
    public boolean existsByUserIdAndEmailIgnoreCaseAndDeletedAtIsNull(UUID userId, String email) {
        return contactJpaRepositoryModule.existsByUserIdAndEmailIgnoreCaseAndDeletedAtIsNull(userId, email);
    }

    @Override
    public boolean existsByUserIdAndEmailIgnoreCaseAndIdNotAndDeletedAtIsNull(
            UUID userId,
            String email,
            UUID excludeId
    ) {
        return contactJpaRepositoryModule.existsByUserIdAndEmailIgnoreCaseAndIdNotAndDeletedAtIsNull(
                userId,
                email,
                excludeId
        );
    }

}
