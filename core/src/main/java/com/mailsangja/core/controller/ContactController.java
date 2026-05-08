package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.ContactControllerDocs;
import com.mailsangja.core.dto.contact.ContactCreateRequest;
import com.mailsangja.core.dto.contact.ContactResponse;
import com.mailsangja.core.dto.contact.ContactUpdateRequest;
import com.mailsangja.core.facade.ContactFacade;
import com.mailsangja.db.entity.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContactController implements ContactControllerDocs {

    private final ContactFacade contactFacade;

    @Override
    @PostMapping("/api/v1/contacts")
    public ResponseEntity<ContactResponse> createContact(
            @AuthUser User user,
            @Valid @RequestBody ContactCreateRequest request
    ) {
        return ResponseEntity.ok(contactFacade.createContact(user, request));
    }

    @Override
    @GetMapping("/api/v1/contacts")
    public ResponseEntity<List<ContactResponse>> getContacts(
            @AuthUser User user,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(contactFacade.getContacts(user, keyword));
    }

    @Override
    @PatchMapping("/api/v1/contacts/{contactId}")
    public ResponseEntity<ContactResponse> updateContact(
            @AuthUser User user,
            @PathVariable UUID contactId,
            @Valid @RequestBody ContactUpdateRequest request
    ) {
        return ResponseEntity.ok(contactFacade.updateContact(user, contactId, request));
    }

    @Override
    @DeleteMapping("/api/v1/contacts/{contactId}")
    public ResponseEntity<Void> deleteContact(
            @AuthUser User user,
            @PathVariable UUID contactId
    ) {
        contactFacade.deleteContact(user, contactId);
        return ResponseEntity.noContent().build();
    }
}
