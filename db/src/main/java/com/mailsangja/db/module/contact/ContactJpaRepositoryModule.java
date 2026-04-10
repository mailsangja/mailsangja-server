package com.mailsangja.db.module.contact;

import com.mailsangja.db.entity.contact.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ContactJpaRepositoryModule extends JpaRepository<Contact, UUID> {
    List<Contact> findAllByEmailInAndDeletedAtIsNull(Collection<String> emails);
}

