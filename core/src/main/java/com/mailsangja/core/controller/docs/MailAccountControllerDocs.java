package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.core.dto.mail.MailAccountResponse;
import com.mailsangja.db.entity.user.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;

public interface MailAccountControllerDocs {

    ResponseEntity<MailAccountAuthorizeResponse> authorizeGoogle(@AuthUser User user, HttpSession session);

    ResponseEntity<MailAccountResponse> googleCallback(@AuthUser User user, String code, String state, HttpSession session);
}
