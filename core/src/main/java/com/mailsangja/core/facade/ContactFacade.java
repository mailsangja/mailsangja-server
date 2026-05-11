package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.dto.contact.ContactCreateRequest;
import com.mailsangja.core.dto.contact.ContactResponse;
import com.mailsangja.core.dto.contact.ContactUpdateRequest;
import com.mailsangja.core.service.contact.ContactCommandService;
import com.mailsangja.core.service.contact.ContactQueryService;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContactFacade {

    private final ContactCommandService contactCommandService;
    private final ContactQueryService contactQueryService;

    public ContactResponse createContact(User user, ContactCreateRequest request) {
        validateCreateRequest(user, request);
        try {
            Contact contact = contactCommandService.create(user, request);
            return ContactResponse.from(contact);
        } catch (DataIntegrityViolationException e) {
            throw new ContactException(ContactErrorCode.CONTACT_EMAIL_DUPLICATE);
        }
    }

    public List<ContactResponse> getContacts(User user, String keyword) {
        validateUser(user);
        if (keyword == null || keyword.isBlank()) {
            return toResponses(contactQueryService.findAllContacts(user));
        }
        return toResponses(contactQueryService.searchContacts(user, keyword.trim()));
    }

    public ContactResponse updateContact(User user, UUID contactId, ContactUpdateRequest request) {
        validateUpdateRequest(user, contactId, request);
        Contact contact = contactQueryService.findActiveContact(user, contactId);
        Contact updated = contactCommandService.updateName(contact, request.name().trim());
        return ContactResponse.from(updated);
    }

    public void deleteContact(User user, UUID contactId) {
        validateContactIdRequest(user, contactId);
        Contact contact = contactQueryService.findActiveContact(user, contactId);
        contactCommandService.delete(contact);
    }

    private List<ContactResponse> toResponses(List<Contact> contacts) {
        List<ContactResponse> responses = new ArrayList<>();
        for (Contact contact : contacts) {
            responses.add(ContactResponse.from(contact));
        }
        return responses;
    }

    private void validateCreateRequest(User user, ContactCreateRequest request) {
        validateUser(user);
        if (request == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }

    private void validateUpdateRequest(User user, UUID contactId, ContactUpdateRequest request) {
        validateContactIdRequest(user, contactId);
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }

    private void validateContactIdRequest(User user, UUID contactId) {
        validateUser(user);
        if (contactId == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }

    private void validateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ContactException(ContactErrorCode.INVALID_CONTACT_REQUEST);
        }
    }
}
