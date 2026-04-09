package com.mailsangja.db.adapter.contact;

import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.module.contact.ContactJpaRepositoryModule;
import com.mailsangja.db.port.ContactRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContactRepositoryAdapter implements ContactRepositoryPort {

    private final ContactJpaRepositoryModule contactJpaRepositoryModule;

    @Override
    public Contact save(Contact contact) {
        return contactJpaRepositoryModule.save(contact);
    }

    @Override
    public List<Contact> findAllByEmailInAndDeletedAtIsNull(List<String> emails) {
        return contactJpaRepositoryModule.findAllByEmailInAndDeletedAtIsNull(emails);
    }
}

