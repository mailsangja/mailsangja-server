package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.mail.MailAccountAuthorizeResponse;
import com.mailsangja.db.entity.user.User;
import org.springframework.http.ResponseEntity;

public interface MailAccountControllerDocs {

    ResponseEntity<MailAccountAuthorizeResponse> authorizeGoogle(@AuthUser User user);

    ResponseEntity<Void> googleCallback(String code, String state);
}
