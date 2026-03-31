package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.MailAccountControllerDocs;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.db.entity.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailAccountController implements MailAccountControllerDocs {

    @Override
    @GetMapping("/api/v1/mail-accounts/google/authorize")
    public ResponseEntity<MailAccountAuthorizeResponse> authorizeGoogle(@AuthUser User user) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    @GetMapping("/api/v1/mail-accounts/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
