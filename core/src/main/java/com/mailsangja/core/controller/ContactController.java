package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.ContactControllerDocs;
import com.mailsangja.core.dto.contact.ContactCreateRequest;
import com.mailsangja.core.dto.contact.ContactResponse;
import com.mailsangja.core.facade.ContactFacade;
import com.mailsangja.db.entity.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
