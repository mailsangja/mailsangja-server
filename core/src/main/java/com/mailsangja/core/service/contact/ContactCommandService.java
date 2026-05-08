package com.mailsangja.core.service.contact;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.dto.contact.ContactCreateRequest;
import com.mailsangja.core.dto.contact.GoogleContactResult;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.ContactRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ContactCommandService {

    private final ContactRepositoryPort contactRepositoryPort;

    @Transactional
    public Contact create(User user, ContactCreateRequest request) {
        validateCreateRequest(user, request);
        Contact contact = Contact.create(user, request.name().trim(), normalizeEmail(request.email()));
        return contactRepositoryPort.save(contact);
    }

    @Transactional
    public Contact updateName(Contact contact, String name) {
        validateUpdateNameRequest(contact, name);
        contact.updateName(name.trim());
        return contactRepositoryPort.save(contact);
    }

    @Transactional
    public void delete(Contact contact) {
        validateContact(contact);
        contact.delete();
        contactRepositoryPort.save(contact);
    }

    @Transactional
    public int saveMissingContacts(User user, List<GoogleContactResult> results) {
        validateSyncRequest(user, results);
        if (results.isEmpty()) {
            return 0;
        }
        return contactRepositoryPort.saveAllIgnoreDuplicateActive(toContacts(user, results));
    }

    private void validateSyncRequest(User user, List<GoogleContactResult> results) {
        if (user == null || user.getId() == null || results == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_SYNC_REQUEST);
        }
    }

    private void validateCreateRequest(User user, ContactCreateRequest request) {
        if (user == null || user.getId() == null || request == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
        if (isBlank(request.name()) || isBlank(request.email())) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }

    private void validateUpdateNameRequest(Contact contact, String name) {
        validateContact(contact);
        if (isBlank(name)) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }

    private void validateContact(Contact contact) {
        if (contact == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }

    private List<Contact> toContacts(User user, List<GoogleContactResult> results) {
        List<Contact> contacts = new ArrayList<>();
        for (GoogleContactResult result : results) {
            contacts.add(Contact.create(user, result.name(), result.email()));
        }
        return contacts;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
