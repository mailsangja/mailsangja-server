package com.mailsangja.core.service.contact;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.dto.contact.GoogleContactResult;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.ContactRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactCommandService {

    private final ContactRepositoryPort contactRepositoryPort;

    @Transactional
    public int saveMissingContacts(User user, List<GoogleContactResult> results) {
        validateSyncRequest(user, results);

        if (results.isEmpty()) {
            return 0;
        }

        List<Contact> contacts = results.stream()
                .map(result -> Contact.builder()
                        .user(user)
                        .name(result.name())
                        .email(result.email())
                        .build())
                .toList();

        return contactRepositoryPort.saveAllIgnoreDuplicateActive(contacts);
    }

    private void validateSyncRequest(User user, List<GoogleContactResult> results) {
        if (user == null || user.getId() == null || results == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_SYNC_REQUEST);
        }
    }
}
