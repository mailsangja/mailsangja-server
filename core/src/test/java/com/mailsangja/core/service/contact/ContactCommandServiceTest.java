package com.mailsangja.core.service.contact;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.dto.contact.GoogleContactResult;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.ContactRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactCommandServiceTest {

    @Test
    void saveMissingContacts_기존활성연락처는덮어쓰지않고신규연락처만저장한다() {
        User user = createUser();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);
        List<GoogleContactResult> results = List.of(
                new GoogleContactResult("기존 구글 이름", "exists@example.com"),
                new GoogleContactResult("새 연락처", "new@example.com"),
                new GoogleContactResult("중복 새 연락처", "new@example.com")
        );
        Contact existingContact = Contact.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("사용자가 저장한 이름")
                .email("exists@example.com")
                .build();
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(
                eq(user.getId()),
                eq(List.of("exists@example.com", "new@example.com"))
        )).thenReturn(List.of(existingContact));
        when(contactRepositoryPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int savedCount = service.saveMissingContacts(user, results);

        assertEquals(1, savedCount);
        ArgumentCaptor<List<Contact>> contactsCaptor = contactListCaptor();
        verify(contactRepositoryPort).saveAll(contactsCaptor.capture());
        List<Contact> savedContacts = contactsCaptor.getValue();
        assertEquals(1, savedContacts.size());
        assertEquals(user, savedContacts.get(0).getUser());
        assertEquals("새 연락처", savedContacts.get(0).getName());
        assertEquals("new@example.com", savedContacts.get(0).getEmail());
    }

    @Test
    void saveMissingContacts_빈결과면저장하지않고0을반환한다() {
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);

        int savedCount = service.saveMissingContacts(createUser(), List.of());

        assertEquals(0, savedCount);
        verify(contactRepositoryPort, never()).saveAll(anyList());
    }

    @Test
    void saveMissingContacts_user가null이면실패한다() {
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);

        ContactException exception = assertThrows(ContactException.class, () -> service.saveMissingContacts(
                null,
                List.of(new GoogleContactResult("Alice", "alice@example.com"))
        ));
        assertEquals(ContactErrorCode.INVALID_CONTACT_SYNC_REQUEST, exception.getErrorCode());
        verify(contactRepositoryPort, never()).saveAll(anyList());
    }

    @Test
    void saveMissingContacts_results가null이면실패한다() {
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);

        ContactException exception = assertThrows(
                ContactException.class,
                () -> service.saveMissingContacts(createUser(), null)
        );
        assertEquals(ContactErrorCode.INVALID_CONTACT_SYNC_REQUEST, exception.getErrorCode());
        verify(contactRepositoryPort, never()).saveAll(anyList());
    }

    @Test
    void saveMissingContacts_blankEmail결과는저장대상에서제외한다() {
        User user = createUser();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(
                eq(user.getId()),
                eq(List.of("valid@example.com"))
        )).thenReturn(List.of());
        when(contactRepositoryPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int savedCount = service.saveMissingContacts(user, List.of(
                new GoogleContactResult("Blank", " "),
                new GoogleContactResult("Valid", "valid@example.com")
        ));

        assertEquals(1, savedCount);
        ArgumentCaptor<List<Contact>> contactsCaptor = contactListCaptor();
        verify(contactRepositoryPort).saveAll(contactsCaptor.capture());
        List<Contact> savedContacts = contactsCaptor.getValue();
        assertEquals(1, savedContacts.size());
        assertEquals(user, savedContacts.get(0).getUser());
        assertEquals("Valid", savedContacts.get(0).getName());
        assertEquals("valid@example.com", savedContacts.get(0).getEmail());
    }

    @Test
    void saveMissingContacts_기존활성연락처는이름을업데이트하지않는다() {
        User user = createUser();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);
        Contact existingContact = Contact.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("사용자가 저장한 이름")
                .email("exists@example.com")
                .build();
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(
                eq(user.getId()),
                eq(List.of("exists@example.com"))
        )).thenReturn(List.of(existingContact));

        int savedCount = service.saveMissingContacts(user, List.of(
                new GoogleContactResult("구글 주소록 이름", "exists@example.com")
        ));

        assertEquals(0, savedCount);
        assertEquals("사용자가 저장한 이름", existingContact.getName());
        verify(contactRepositoryPort, never()).saveAll(anyList());
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<Contact>> contactListCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
