package com.mailsangja.core.service.contact;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.ContactRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactQueryServiceTest {

    @Test
    void findAllContacts_사용자의activeContact전체조회포트를호출한다() {
        // given
        User user = createUser();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactQueryService service = new ContactQueryService(contactRepositoryPort);
        List<Contact> contacts = List.of(createContact(user, "Alice", "alice@example.com"));
        when(contactRepositoryPort.findAllByUserIdAndDeletedAtIsNull(user.getId())).thenReturn(contacts);

        // when
        List<Contact> result = service.findAllContacts(user);

        // then
        assertEquals(contacts, result);
        verify(contactRepositoryPort).findAllByUserIdAndDeletedAtIsNull(user.getId());
        verify(contactRepositoryPort, never()).findAllByUserIdAndKeywordAndDeletedAtIsNull(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void searchContacts_keyword가있으면검색어로nameEmail검색포트를호출한다() {
        // given
        User user = createUser();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactQueryService service = new ContactQueryService(contactRepositoryPort);
        List<Contact> contacts = List.of(createContact(user, "Alice", "alice@example.com"));
        when(contactRepositoryPort.findAllByUserIdAndKeywordAndDeletedAtIsNull(user.getId(), "alice"))
                .thenReturn(contacts);

        // when
        List<Contact> result = service.searchContacts(user, "alice");

        // then
        assertEquals(contacts, result);
        verify(contactRepositoryPort).findAllByUserIdAndKeywordAndDeletedAtIsNull(user.getId(), "alice");
        verify(contactRepositoryPort, never()).findAllByUserIdAndDeletedAtIsNull(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void findAllContacts_user가null이면실패한다() {
        // given
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactQueryService service = new ContactQueryService(contactRepositoryPort);

        // when
        ContactException exception = assertThrows(ContactException.class, () -> service.findAllContacts(null));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactRepositoryPort, never()).findAllByUserIdAndDeletedAtIsNull(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void searchContacts_user가null이면실패한다() {
        // given
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactQueryService service = new ContactQueryService(contactRepositoryPort);

        // when
        ContactException exception = assertThrows(ContactException.class, () -> service.searchContacts(null, "alice"));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactRepositoryPort, never()).findAllByUserIdAndKeywordAndDeletedAtIsNull(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void searchContacts_keyword가blank이면실패한다() {
        // given
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactQueryService service = new ContactQueryService(contactRepositoryPort);

        // when
        ContactException exception = assertThrows(ContactException.class, () -> service.searchContacts(createUser(), "   "));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactRepositoryPort, never()).findAllByUserIdAndKeywordAndDeletedAtIsNull(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void findActiveContact_contactId와userId로activeContact를조회한다() {
        // given
        User user = createUser();
        UUID contactId = UUID.randomUUID();
        Contact contact = createContact(user, "Alice", "alice@example.com");
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactQueryService service = new ContactQueryService(contactRepositoryPort);
        when(contactRepositoryPort.findByIdAndUserIdAndDeletedAtIsNull(contactId, user.getId()))
                .thenReturn(Optional.of(contact));

        // when
        Contact result = service.findActiveContact(user, contactId);

        // then
        assertEquals(contact, result);
        verify(contactRepositoryPort).findByIdAndUserIdAndDeletedAtIsNull(contactId, user.getId());
    }

    @Test
    void findActiveContact_조회결과가없으면실패한다() {
        // given
        User user = createUser();
        UUID contactId = UUID.randomUUID();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactQueryService service = new ContactQueryService(contactRepositoryPort);
        when(contactRepositoryPort.findByIdAndUserIdAndDeletedAtIsNull(contactId, user.getId()))
                .thenReturn(Optional.empty());

        // when
        ContactException exception = assertThrows(
                ContactException.class,
                () -> service.findActiveContact(user, contactId)
        );

        // then
        assertEquals(ContactErrorCode.CONTACT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findActiveContact_contactId가null이면실패한다() {
        // given
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactQueryService service = new ContactQueryService(contactRepositoryPort);

        // when
        ContactException exception = assertThrows(
                ContactException.class,
                () -> service.findActiveContact(createUser(), null)
        );

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
    }

    private User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("사용자")
                .username("user@example.com")
                .password("password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .build();
    }

    private Contact createContact(User user, String name, String email) {
        return Contact.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name(name)
                .email(email)
                .build();
    }
}
