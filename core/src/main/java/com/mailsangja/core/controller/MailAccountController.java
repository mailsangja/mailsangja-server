package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.controller.docs.MailAccountControllerDocs;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeRequest;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.facade.MailAccountFacade;
import com.mailsangja.db.entity.user.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MailAccountController implements MailAccountControllerDocs {

    private static final String GOOGLE_OAUTH_STATE = "google_oauth_state";
    private static final String GOOGLE_OAUTH_USER_ID = "google_oauth_user_id";
    private static final String GOOGLE_OAUTH_ALIAS = "google_oauth_alias";
    private static final String GOOGLE_OAUTH_ICON = "google_oauth_icon";
    private static final String GOOGLE_OAUTH_COLOR = "google_oauth_color";

    private final MailAccountFacade mailAccountFacade;

    @Override
    @GetMapping("/api/v1/mail-accounts/google/authorize")
    public ResponseEntity<MailAccountAuthorizeResponse> authorizeGoogle(
            @AuthUser User user,
            @ModelAttribute MailAccountAuthorizeRequest request,
            HttpSession session
    ) {
        String state = UUID.randomUUID().toString();
        session.setAttribute(GOOGLE_OAUTH_STATE, state);
        session.setAttribute(GOOGLE_OAUTH_USER_ID, user.getId().toString());
        session.setAttribute(GOOGLE_OAUTH_ALIAS, request.alias());
        session.setAttribute(GOOGLE_OAUTH_ICON, request.icon());
        session.setAttribute(GOOGLE_OAUTH_COLOR, request.color());

        return ResponseEntity.ok(mailAccountFacade.authorizeGoogle(state, request));
    }

    @Override
    @GetMapping("/api/v1/mail-accounts/google/callback")
    public ResponseEntity<Void> googleCallback(
            @AuthUser User user,
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpSession session
    ) {
        String savedState = (String) session.getAttribute(GOOGLE_OAUTH_STATE);
        String savedUserId = (String) session.getAttribute(GOOGLE_OAUTH_USER_ID);
        String savedAlias = (String) session.getAttribute(GOOGLE_OAUTH_ALIAS);
        String savedIcon = (String) session.getAttribute(GOOGLE_OAUTH_ICON);
        String savedColor = (String) session.getAttribute(GOOGLE_OAUTH_COLOR);

        if (savedState == null || savedAlias == null || savedIcon == null || savedColor == null) {
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
        session.removeAttribute(GOOGLE_OAUTH_ALIAS);
        session.removeAttribute(GOOGLE_OAUTH_ICON);
        session.removeAttribute(GOOGLE_OAUTH_COLOR);

        mailAccountFacade.handleGoogleCallback(user, code, savedAlias, savedIcon, savedColor);

        return ResponseEntity.status(302)
                .location(URI.create("/"))
                .build();
    }
}
