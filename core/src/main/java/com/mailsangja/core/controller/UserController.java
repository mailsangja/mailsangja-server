package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.UserControllerDocs;
import com.mailsangja.core.dto.auth.UserInfoResponse;
import com.mailsangja.core.dto.user.UpdateDefaultAccountRequest;
import com.mailsangja.core.facade.UserFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserFacade userFacade;

    @Override
    @GetMapping("/api/v1/users/me")
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthUser User user) {
        return ResponseEntity.ok(userFacade.getUserInfo(user));
    }

    @Override
    @PatchMapping("/api/v1/users/me/default-account")
    public ResponseEntity<Void> updateDefaultAccount(@AuthUser User user, @RequestBody UpdateDefaultAccountRequest request) {
        userFacade.updateDefaultAccount(user, request);
        return ResponseEntity.ok().build();
    }
}
