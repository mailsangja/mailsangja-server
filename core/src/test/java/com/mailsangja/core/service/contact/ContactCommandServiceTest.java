package com.mailsangja.core.service.contact;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.dto.contact.ContactCreateRequest;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactCommandServiceTest {

    @Test
    void create_요청값으로연락처를저장하고Contact를반환한다() {
        // given
        User user = createUser();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);
        ContactCreateRequest request = new ContactCreateRequest(" Alice ", " Alice@Example.COM ");

        when(contactRepositoryPort.save(org.mockito.ArgumentMatchers.any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Contact saved = service.create(user, request);

        // then
        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepositoryPort).save(contactCaptor.capture());
        Contact persisted = contactCaptor.getValue();
        assertEquals(user, persisted.getUser());
        assertEquals("Alice", persisted.getName());
        assertEquals("alice@example.com", persisted.getEmail());
        assertEquals(persisted, saved);
    }

    @Test
    void create_user가null이면실패한다() {
        // given
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);
        ContactCreateRequest request = new ContactCreateRequest("Alice", "alice@example.com");

        // when
        ContactException exception = assertThrows(ContactException.class, () -> service.create(null, request));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any(Contact.class));
    }

    @Test
    void create_request가null이면실패한다() {
        // given
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);

        // when
        ContactException exception = assertThrows(ContactException.class, () -> service.create(createUser(), null));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any(Contact.class));
    }

    @Test
    void saveMissingContacts_정제된결과를중복무시저장포트로위임한다() {
        User user = createUser();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);
        List<GoogleContactResult> results = List.of(
                new GoogleContactResult("기존 구글 이름", "exists@example.com"),
                new GoogleContactResult("새 연락처", "new@example.com")
        );
        when(contactRepositoryPort.saveAllIgnoreDuplicateActive(anyList())).thenReturn(1);

        int savedCount = service.saveMissingContacts(user, results);

        assertEquals(1, savedCount);
        ArgumentCaptor<List<Contact>> contactsCaptor = contactListCaptor();
        verify(contactRepositoryPort).saveAllIgnoreDuplicateActive(contactsCaptor.capture());
        List<Contact> savedContacts = contactsCaptor.getValue();
        assertEquals(2, savedContacts.size());
        assertEquals(user, savedContacts.get(0).getUser());
        assertEquals("기존 구글 이름", savedContacts.get(0).getName());
        assertEquals("exists@example.com", savedContacts.get(0).getEmail());
        assertEquals(user, savedContacts.get(1).getUser());
        assertEquals("새 연락처", savedContacts.get(1).getName());
        assertEquals("new@example.com", savedContacts.get(1).getEmail());
    }

    @Test
    void saveMissingContacts_빈결과면저장하지않고0을반환한다() {
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);

        int savedCount = service.saveMissingContacts(createUser(), List.of());

        assertEquals(0, savedCount);
        verify(contactRepositoryPort, never()).saveAllIgnoreDuplicateActive(anyList());
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
        verify(contactRepositoryPort, never()).saveAllIgnoreDuplicateActive(anyList());
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
        verify(contactRepositoryPort, never()).saveAllIgnoreDuplicateActive(anyList());
    }

    @Test
    void saveMissingContacts_이미존재하는연락처는DB고유제약으로무시된저장개수를반환한다() {
        User user = createUser();
        ContactRepositoryPort contactRepositoryPort = mock(ContactRepositoryPort.class);
        ContactCommandService service = new ContactCommandService(contactRepositoryPort);
        when(contactRepositoryPort.saveAllIgnoreDuplicateActive(anyList())).thenReturn(0);

        int savedCount = service.saveMissingContacts(user, List.of(
                new GoogleContactResult("구글 주소록 이름", "exists@example.com")
        ));

        assertEquals(0, savedCount);
        verify(contactRepositoryPort).saveAllIgnoreDuplicateActive(anyList());
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
