package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.controller.docs.MailAccountControllerDocs;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.core.facade.MailAccountFacade;
import com.mailsangja.db.entity.user.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MailAccountController implements MailAccountControllerDocs {

    private static final String GOOGLE_OAUTH_STATE = "google_oauth_state";
    private static final String GOOGLE_OAUTH_USER_ID = "google_oauth_user_id";

    private final MailAccountFacade mailAccountFacade;

    @Override
    @GetMapping("/api/v1/mail-accounts/google/authorize")
    public ResponseEntity<MailAccountAuthorizeResponse> authorizeGoogle(
            @AuthUser User user,
            HttpSession session
    ) {
        String state = UUID.randomUUID().toString();
        session.setAttribute(GOOGLE_OAUTH_STATE, state);
        session.setAttribute(GOOGLE_OAUTH_USER_ID, user.getId().toString());

        return ResponseEntity.ok(mailAccountFacade.authorizeGoogle(state));
    }

    @Override
    @GetMapping("/api/v1/mail-accounts/google/callback")
    public ResponseEntity<MailAccountResponse> googleCallback(
            @AuthUser User user,
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpSession session
    ) {
        String savedState = (String) session.getAttribute(GOOGLE_OAUTH_STATE);
        String savedUserId = (String) session.getAttribute(GOOGLE_OAUTH_USER_ID);

        if (savedState == null) {
            throw new MailAccountException(MailAccountErrorCode.OAUTH_SESSION_NOT_FOUND);
        }

        if (savedUserId == null || !user.getId().toString().equals(savedUserId)) {
            throw new MailAccountException(MailAccountErrorCode.OAUTH_USER_MISMATCH);
        }

        if (!savedState.equals(state)) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_STATE);
        }

        session.removeAttribute(GOOGLE_OAUTH_STATE);
        session.removeAttribute(GOOGLE_OAUTH_USER_ID);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mailAccountFacade.handleGoogleCallback(user, code));
    }
}
