package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.MailControllerDocs;
import com.mailsangja.core.dto.mail.MailComposeResponse;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.facade.MailFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MailController implements MailControllerDocs {

    private final MailFacade mailFacade;

    @Override
    @PostMapping(value = "/api/v1/mail/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> sendMail(@AuthUser User user, @ModelAttribute MailSendRequest request) {
        mailFacade.sendMail(user, request);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/api/v1/mail/composes")
    public ResponseEntity<MailComposeResponse> createCompose(@AuthUser User user) {
        return ResponseEntity.status(201)
                .body(mailFacade.createCompose(user));
    }
}
