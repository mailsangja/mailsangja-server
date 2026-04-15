package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.FcmTokenControllerDocs;
import com.mailsangja.core.dto.fcm.RegisterFcmTokenRequest;
import com.mailsangja.core.facade.FcmFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FcmTokenController implements FcmTokenControllerDocs {

    private final FcmFacade fcmFacade;

    @Override
    @PostMapping("/api/v1/users/me/fcm-token")
    public ResponseEntity<Void> registerFcmToken(
            @AuthUser User user,
            @RequestBody RegisterFcmTokenRequest request
    ) {
        fcmFacade.registerFcmToken(user, request);
        return ResponseEntity.ok().build();
    }
}
