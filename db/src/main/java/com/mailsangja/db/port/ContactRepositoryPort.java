package com.mailsangja.db.port;

import com.mailsangja.db.entity.contact.Contact;

import java.util.List;

public interface ContactRepositoryPort {
    Contact save(Contact contact);
    List<Contact> findAllByEmailInAndDeletedAtIsNull(List<String> emails);
}
