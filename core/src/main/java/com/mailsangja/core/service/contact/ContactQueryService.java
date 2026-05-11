package com.mailsangja.core.service.contact;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.ContactRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactQueryService {

    private final ContactRepositoryPort contactRepositoryPort;

    public List<Contact> findAllContacts(User user) {
        validateUser(user);
        return contactRepositoryPort.findAllByUserIdAndDeletedAtIsNull(user.getId());
    }

    public List<Contact> searchContacts(User user, String keyword) {
        validateSearchRequest(user, keyword);
        return contactRepositoryPort.findAllByUserIdAndKeywordAndDeletedAtIsNull(user.getId(), keyword);
    }

    public Contact findActiveContact(User user, UUID contactId) {
        validateContactRequest(user, contactId);
        Optional<Contact> contact = contactRepositoryPort.findByIdAndUserIdAndDeletedAtIsNull(contactId, user.getId());
        if (contact.isEmpty()) {
            throw new ContactException(ContactErrorCode.CONTACT_NOT_FOUND);
        }
        return contact.get();
    }

    private void validateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }

    private void validateSearchRequest(User user, String keyword) {
        validateUser(user);
        if (keyword == null || keyword.isBlank()) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }

    private void validateContactRequest(User user, UUID contactId) {
        validateUser(user);
        if (contactId == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }
}
