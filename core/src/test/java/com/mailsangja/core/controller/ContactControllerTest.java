package com.mailsangja.core.controller;

import com.mailsangja.core.dto.contact.ContactCreateRequest;
import com.mailsangja.core.dto.contact.ContactResponse;
import com.mailsangja.core.facade.ContactFacade;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactControllerTest {

    @Test
    void createContact_POST_api_v1_contacts는Facade를호출하고응답을반환한다() {
        // given
        ContactFacade contactFacade = mock(ContactFacade.class);
        ContactController controller = new ContactController(contactFacade);
        User user = createUser();
        ContactCreateRequest request = new ContactCreateRequest("Alice", "alice@example.com");
        ContactResponse expected = new ContactResponse(UUID.randomUUID(), "Alice", "alice@example.com");
        when(contactFacade.createContact(user, request)).thenReturn(expected);

        // when
        ResponseEntity<ContactResponse> response = controller.createContact(user, request);

        // then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(contactFacade).createContact(user, request);
    }

    @Test
    void getContacts_GET_api_v1_contacts는Facade를호출하고List응답을반환한다() {
        // given
        ContactFacade contactFacade = mock(ContactFacade.class);
        ContactController controller = new ContactController(contactFacade);
        User user = createUser();
        List<ContactResponse> expected = List.of(
                new ContactResponse(UUID.randomUUID(), "Alice", "alice@example.com"),
                new ContactResponse(UUID.randomUUID(), "Bob", "bob@example.com")
        );
        when(contactFacade.getContacts(user, "ali")).thenReturn(expected);

        // when
        ResponseEntity<List<ContactResponse>> response = controller.getContacts(user, "ali");

        // then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(contactFacade).getContacts(user, "ali");
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
}
