package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.contact.ContactErrorCode;
import com.mailsangja.core.common.exception.contact.ContactException;
import com.mailsangja.core.dto.contact.ContactCreateRequest;
import com.mailsangja.core.dto.contact.ContactResponse;
import com.mailsangja.core.dto.contact.ContactUpdateRequest;
import com.mailsangja.core.service.contact.ContactCommandService;
import com.mailsangja.core.service.contact.ContactQueryService;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactFacadeTest {

    @Test
    void createContact_정상요청이면생성된연락처응답을반환한다() {
        // given
        User user = createUser();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        ContactCreateRequest request = new ContactCreateRequest("Alice", "alice@example.com");
        Contact contact = createContact(user, "Alice", "alice@example.com");
        when(contactCommandService.create(user, request)).thenReturn(contact);

        // when
        ContactResponse response = facade.createContact(user, request);

        // then
        assertEquals(contact.getId(), response.id());
        assertEquals("Alice", response.name());
        assertEquals("alice@example.com", response.email());
        verify(contactCommandService).create(user, request);
        verify(contactQueryService, never()).findAllContacts(
                org.mockito.ArgumentMatchers.any()
        );
        verify(contactQueryService, never()).searchContacts(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void createContact_user가null이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        ContactCreateRequest request = new ContactCreateRequest("Alice", "alice@example.com");

        // when
        ContactException exception = assertThrows(ContactException.class, () -> facade.createContact(null, request));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactCommandService, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void createContact_request가null이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);

        // when
        ContactException exception = assertThrows(ContactException.class, () -> facade.createContact(createUser(), null));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactCommandService, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void createContact_중복email제약위반이면ContactException으로변환한다() {
        // given
        User user = createUser();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        ContactCreateRequest request = new ContactCreateRequest("Alice", "ALICE@example.com");
        when(contactCommandService.create(user, request)).thenThrow(new DataIntegrityViolationException("duplicate"));

        // when
        ContactException exception = assertThrows(ContactException.class, () -> facade.createContact(user, request));

        // then
        assertEquals(ContactErrorCode.CONTACT_EMAIL_DUPLICATE, exception.getErrorCode());
        verify(contactCommandService).create(user, request);
    }

    @Test
    void getContacts_keyword없이조회하면연락처응답목록을반환한다() {
        // given
        User user = createUser();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        Contact alice = createContact(user, "Alice", "alice@example.com");
        Contact bob = createContact(user, "Bob", "bob@example.com");
        when(contactQueryService.findAllContacts(user)).thenReturn(List.of(alice, bob));

        // when
        List<ContactResponse> responses = facade.getContacts(user, null);

        // then
        assertEquals(2, responses.size());
        assertEquals(alice.getId(), responses.get(0).id());
        assertEquals("Alice", responses.get(0).name());
        assertEquals("alice@example.com", responses.get(0).email());
        assertEquals(bob.getId(), responses.get(1).id());
        assertEquals("Bob", responses.get(1).name());
        assertEquals("bob@example.com", responses.get(1).email());
        verify(contactQueryService).findAllContacts(user);
        verify(contactQueryService, never()).searchContacts(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(contactCommandService, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getContacts_keyword가blank이면전체연락처응답목록을반환한다() {
        // given
        User user = createUser();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        Contact alice = createContact(user, "Alice", "alice@example.com");
        when(contactQueryService.findAllContacts(user)).thenReturn(List.of(alice));

        // when
        List<ContactResponse> responses = facade.getContacts(user, "   ");

        // then
        assertEquals(1, responses.size());
        assertEquals(alice.getId(), responses.getFirst().id());
        verify(contactQueryService).findAllContacts(user);
        verify(contactQueryService, never()).searchContacts(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void getContacts_keyword로조회하면검색된연락처응답목록을반환한다() {
        // given
        User user = createUser();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        Contact alice = createContact(user, "Alice", "alice@example.com");
        when(contactQueryService.searchContacts(user, "ali")).thenReturn(List.of(alice));

        // when
        List<ContactResponse> responses = facade.getContacts(user, "  ali  ");

        // then
        assertEquals(1, responses.size());
        assertEquals(alice.getId(), responses.getFirst().id());
        assertEquals("Alice", responses.getFirst().name());
        assertEquals("alice@example.com", responses.getFirst().email());
        verify(contactQueryService).searchContacts(user, "ali");
        verify(contactQueryService, never()).findAllContacts(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getContacts_user가null이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);

        // when
        ContactException exception = assertThrows(ContactException.class, () -> facade.getContacts(null, null));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactQueryService, never()).findAllContacts(
                org.mockito.ArgumentMatchers.any()
        );
        verify(contactQueryService, never()).searchContacts(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void updateContact_정상요청이면활성연락처를조회하고이름수정응답을반환한다() {
        // given
        User user = createUser();
        UUID contactId = UUID.randomUUID();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        ContactUpdateRequest request = new ContactUpdateRequest(" Alice Updated ");
        Contact contact = createContact(user, "Alice", "alice@example.com");
        Contact updated = createContact(user, "Alice Updated", "alice@example.com");
        when(contactQueryService.findActiveContact(user, contactId)).thenReturn(contact);
        when(contactCommandService.updateName(contact, "Alice Updated")).thenReturn(updated);

        // when
        ContactResponse response = facade.updateContact(user, contactId, request);

        // then
        assertEquals(updated.getId(), response.id());
        assertEquals("Alice Updated", response.name());
        assertEquals("alice@example.com", response.email());
        verify(contactQueryService).findActiveContact(user, contactId);
        verify(contactCommandService).updateName(contact, "Alice Updated");
    }

    @Test
    void updateContact_request가null이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);

        // when
        ContactException exception = assertThrows(
                ContactException.class,
                () -> facade.updateContact(createUser(), UUID.randomUUID(), null)
        );

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactQueryService, never()).findActiveContact(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void updateContact_user가null이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        ContactUpdateRequest request = new ContactUpdateRequest("Alice");

        // when
        ContactException exception = assertThrows(ContactException.class, () -> facade.updateContact(null,UUID.randomUUID(), request));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactCommandService, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void updateContact_contactId가null이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        ContactUpdateRequest request = new ContactUpdateRequest("Alice");

        // when
        ContactException exception = assertThrows(
                ContactException.class,
                () -> facade.updateContact(createUser(), null, request)
        );

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactQueryService, never()).findActiveContact(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void updateContact_name이blank이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        ContactUpdateRequest request = new ContactUpdateRequest("   ");

        // when
        ContactException exception = assertThrows(
                ContactException.class,
                () -> facade.updateContact(createUser(), UUID.randomUUID(), request)
        );

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactQueryService, never()).findActiveContact(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void updateContact_대상이없으면ContactNotFound를전파한다() {
        // given
        User user = createUser();
        UUID contactId = UUID.randomUUID();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        ContactUpdateRequest request = new ContactUpdateRequest("Alice");
        when(contactQueryService.findActiveContact(user, contactId))
                .thenThrow(new ContactException(ContactErrorCode.CONTACT_NOT_FOUND));

        // when
        ContactException exception = assertThrows(
                ContactException.class,
                () -> facade.updateContact(user, contactId, request)
        );

        // then
        assertEquals(ContactErrorCode.CONTACT_NOT_FOUND, exception.getErrorCode());
        verify(contactCommandService, never()).updateName(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void deleteContact_정상요청이면활성연락처를조회하고삭제한다() {
        // given
        User user = createUser();
        UUID contactId = UUID.randomUUID();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        Contact contact = createContact(user, "Alice", "alice@example.com");
        when(contactQueryService.findActiveContact(user, contactId)).thenReturn(contact);

        // when
        facade.deleteContact(user, contactId);

        // then
        verify(contactQueryService).findActiveContact(user, contactId);
        verify(contactCommandService).delete(contact);
    }


    @Test
    void deleteContact_user가null이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);

        // when
        ContactException exception = assertThrows(ContactException.class, () -> facade.deleteContact(null,UUID.randomUUID()));

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactCommandService, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void deleteContact_contactId가null이면실패한다() {
        // given
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);

        // when
        ContactException exception = assertThrows(
                ContactException.class,
                () -> facade.deleteContact(createUser(), null)
        );

        // then
        assertEquals(ContactErrorCode.INVALID_CONTACT_REQUEST, exception.getErrorCode());
        verify(contactQueryService, never()).findActiveContact(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void deleteContact_대상이없으면ContactNotFound를전파한다() {
        // given
        User user = createUser();
        UUID contactId = UUID.randomUUID();
        ContactCommandService contactCommandService = mock(ContactCommandService.class);
        ContactQueryService contactQueryService = mock(ContactQueryService.class);
        ContactFacade facade = new ContactFacade(contactCommandService, contactQueryService);
        when(contactQueryService.findActiveContact(user, contactId))
                .thenThrow(new ContactException(ContactErrorCode.CONTACT_NOT_FOUND));

        // when
        ContactException exception = assertThrows(
                ContactException.class,
                () -> facade.deleteContact(user, contactId)
        );

        // then
        assertEquals(ContactErrorCode.CONTACT_NOT_FOUND, exception.getErrorCode());
        verify(contactCommandService, never()).delete(org.mockito.ArgumentMatchers.any());
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
